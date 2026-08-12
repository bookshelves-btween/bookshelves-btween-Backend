package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.service.AIQuestionPreparationService;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.book.service.BookCommandService.PreparedBook;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingParticipationResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.event.MeetingCreatedEvent;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
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
  private final TransactionTemplate transactionTemplate;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public MeetingCreateResDTO createMeeting(MeetingCreateReqDTO request) {
    PreparedBook preparedBook = bookCommandService.prepareBook(request.isbn());
    Long memberId = authenticationFacade.getCurrentMemberId();

    return transactionTemplate.execute(
        status -> {
          Book book = bookCommandService.persistPreparedBook(preparedBook);
          return saveMeeting(request, book, memberId);
        });
  }

  private MeetingCreateResDTO saveMeeting(MeetingCreateReqDTO request, Book book, Long memberId) {
    Meeting meeting = MeetingConverter.toEntity(book, request);
    Meeting savedMeeting = meetingRepository.save(meeting);
    chatRoomRepository.save(ChatRoom.create(savedMeeting));

    Member leader = memberRepository.getReferenceById(memberId);
    meetingParticipantRepository.save(MeetingParticipant.createLeader(savedMeeting, leader));
    savedMeeting.addParticipant();

    eventPublisher.publishEvent(
        new MeetingCreatedEvent(savedMeeting.getId(), savedMeeting.getStartDate()));

    return MeetingCreateResDTO.from(savedMeeting);
  }

  // 마감 상태를 커밋한 뒤 참여 요청에는 마감 예외를 반환한다.
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

    // 정원 충족과 모집 기한 도달은 같은 마감 이벤트로 합류한다.
    if (meeting.getStatus() == MeetingStatus.RECRUIT_CLOSED) {
      eventPublisher.publishEvent(new MeetingRecruitClosedEvent(meetingId));
    }

    return MeetingParticipationResDTO.from(meetingParticipant);
  }

  @Transactional
  public boolean startMeeting(Long meetingId, LocalDateTime now) {
    // 중복 실행에도 한 번만 시작되도록 행을 잠근 뒤 상태를 확인한다.
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || (meeting.getStatus() != MeetingStatus.RECRUITING
            && meeting.getStatus() != MeetingStatus.RECRUIT_CLOSED)
        || !meeting.canStart()
        || meeting.getStartDate().isAfter(now)) {
      return false;
    }

    meeting.start();
    // 마감 이벤트에서 준비되지 않은 질문은 시드 문장으로 보충한다.
    aiQuestionPreparationService.ensureSeeded(meetingId);

    List<MeetingParticipant> participants =
        meetingParticipantRepository.findAllWithMemberByMeetingId(meetingId);
    notificationCommandService.createNotifications(
        participants.stream()
            .map(participant -> Notification.meetingStarted(participant.getMember(), meeting))
            .toList());
    return true;
  }

  @Transactional
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
      eventPublisher.publishEvent(new MeetingRecruitClosedEvent(meetingId));
      return;
    }

    // 삭제 전에 취소 알림을 영속화한다.
    List<MeetingParticipant> participants =
        meetingParticipantRepository.findAllWithMemberByMeetingId(meetingId);
    notificationCommandService.createNotifications(
        participants.stream()
            .map(participant -> Notification.meetingCanceled(participant.getMember(), meeting))
            .toList());

    // FK 의존 순서에 따라 모임의 하위 데이터를 먼저 삭제한다.
    aiQuestionRepository.deleteAllByMeetingId(meetingId);
    reportRepository.deleteAllByMeetingId(meetingId);
    chatRoomRepository.deleteAllByMeetingId(meetingId);
    meetingParticipantRepository.deleteAllByMeetingId(meetingId);
    meetingRepository.delete(meeting);
  }

  // 최초 유효 구독을 출석으로 기록하며 이후 해제해도 되돌리지 않는다.
  @Transactional
  public void markAttended(Long chatroomId, Long memberId) {
    meetingParticipantRepository.markAttendedByChatroom(chatroomId, memberId);
  }
}
