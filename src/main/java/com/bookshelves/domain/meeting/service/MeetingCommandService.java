package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.service.AIQuestionPreparationService;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingParticipationResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.event.MeetingRecruitClosedEvent;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.report.repository.ReportRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import com.bookshelves.global.util.ServiceTime;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MeetingCommandService {

  private final BookCommandService bookCommandService;
  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final NotificationCommandService notificationCommandService;
  private final MemberRepository memberRepository;
  private final AuthenticationFacade authenticationFacade;
  private final AIQuestionRepository aiQuestionRepository;
  private final ReportRepository reportRepository;
  private final AIQuestionPreparationService aiQuestionPreparationService;
  private final ApplicationEventPublisher eventPublisher;

  public MeetingCreateResDTO createMeeting(MeetingCreateReqDTO request) {
    Book book = bookCommandService.getOrCreateByIsbn(request.isbn());

    Meeting meeting = MeetingConverter.toEntity(book, request);
    Meeting savedMeeting = meetingRepository.save(meeting);
    // 모임 생성과 채팅방 생성을 같은 트랜잭션으로 처리한다.
    chatRoomRepository.save(ChatRoom.create(savedMeeting));

    Long memberId = authenticationFacade.getCurrentMemberId();
    Member leader = memberRepository.getReferenceById(memberId);
    meetingParticipantRepository.save(MeetingParticipant.createLeader(savedMeeting, leader));
    savedMeeting.addParticipant();

    return MeetingCreateResDTO.from(savedMeeting);
  }

  // 마감 처리를 완료한 뒤 모집 마감 예외를 반환하므로 MeetingException에도 변경사항을 커밋한다.
  @Transactional(noRollbackFor = MeetingException.class)
  public MeetingParticipationResDTO participateMeeting(Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

    if (meeting.getStatus() != MeetingStatus.RECRUITING
        || meeting.getCurParticipants() >= meeting.getMaxParticipants()) {
      throw new MeetingException(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    }
    if (meeting.isRecruitmentClosedAt(ServiceTime.now())) {
      completeRecruitmentDeadline(meetingId, meeting);
      throw new MeetingException(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    }

    Long memberId = authenticationFacade.getCurrentMemberId();
    if (meetingParticipantRepository.existsByMeetingIdAndMemberId(meetingId, memberId)) {
      throw new MeetingException(MeetingErrorCode.DUPLICATE_MEETING);
    }

    Member member = memberRepository.getReferenceById(memberId);

    MeetingParticipant meetingParticipant =
        meetingParticipantRepository.save(MeetingParticipant.create(meeting, member));
    meeting.addParticipant();

    // 정원 충족으로 모집이 마감되면 모임 성립이 확정된다 — AI 질문 준비를 이 시점에 시작한다.
    // 또 하나의 마감 경로인 `starts_at - 6h`는 completeRecruitmentDeadline에서 같은 이벤트를 발행한다.
    if (meeting.getStatus() == MeetingStatus.RECRUIT_CLOSED) {
      eventPublisher.publishEvent(new MeetingRecruitClosedEvent(meetingId));
    }

    return MeetingParticipationResDTO.from(meetingParticipant);
  }

  public boolean startMeeting(Long meetingId, LocalDateTime now) {
    // 스케줄러 중복 실행에도 한 번만 상태가 변경되도록 잠금 후 다시 확인한다.
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || (meeting.getStatus() != MeetingStatus.RECRUITING
            && meeting.getStatus() != MeetingStatus.RECRUIT_CLOSED)
        || !meeting.canStart()
        || meeting.getStartDate().isAfter(now)) {
      return false;
    }

    meeting.start();
    // 모집 마감 경로를 타지 않고 시작된 모임(정원 미충족 등)을 위한 안전망 —
    // 시작과 동시에 1번 질문이 공개되므로 질문 5개가 반드시 있어야 한다. LLM은 호출하지 않는다.
    aiQuestionPreparationService.ensureSeeded(meetingId);

    List<MeetingParticipant> participants =
        meetingParticipantRepository.findAllWithMemberByMeetingId(meetingId);
    notificationCommandService.saveAll(
        participants.stream()
            .map(participant -> Notification.meetingStarted(participant.getMember(), meeting))
            .toList());
    return true;
  }

  public boolean processRecruitmentDeadline(Long meetingId, LocalDateTime now) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || meeting.getStatus() != MeetingStatus.RECRUITING
        || meeting.getRecruitmentCloseDate().isAfter(now)) {
      return false;
    }

    completeRecruitmentDeadline(meetingId, meeting);
    return true;
  }

  private void completeRecruitmentDeadline(Long meetingId, Meeting meeting) {
    if (meeting.canStart()) {
      meeting.closeRecruitment();
      // 마감 시각 도달로 모임 성립이 확정된 경로 — 정원 충족 경로와 같은 이벤트로 합류시킨다
      eventPublisher.publishEvent(new MeetingRecruitClosedEvent(meetingId));
      return;
    }

    // 모임을 삭제하기 전에 모든 참여자의 취소 알림을 영속화한다.
    List<MeetingParticipant> participants =
        meetingParticipantRepository.findAllWithMemberByMeetingId(meetingId);
    notificationCommandService.saveAll(
        participants.stream()
            .map(participant -> Notification.meetingCanceled(participant.getMember(), meeting))
            .toList());

    // 모집 마감 시점에 AI 질문이 준비됐을 수 있다 — meeting_id FK가 걸려 있어 모임보다 먼저 지운다
    aiQuestionRepository.deleteAllByMeetingId(meetingId);
    // 신고가 채팅방을 참조하므로 FK 의존 순서에 따라 채팅방보다 먼저 지운다.
    reportRepository.deleteAllByMeetingId(meetingId);
    chatRoomRepository.deleteAllByMeetingId(meetingId);
    meetingParticipantRepository.deleteAllByMeetingId(meetingId);
    meetingRepository.delete(meeting);
  }

  // 채팅방 최초 유효 구독 시 출석 처리("1회 이상 입장 = 출석"). 이미 true면 멱등하게 무시하며,
  // 한번 true가 되면 재접속·해제로 되돌리지 않는다. 모임 종료 시 attended != true가 노쇼로 확정된다.
  public void markAttended(Long chatroomId, Long memberId) {
    meetingParticipantRepository.markAttendedByChatroom(chatroomId, memberId);
  }
}
