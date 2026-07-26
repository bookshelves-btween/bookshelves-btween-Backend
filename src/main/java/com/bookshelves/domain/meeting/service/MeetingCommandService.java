package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.converter.MeetingConverter;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingParticipationResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
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
  private final MemberRepository memberRepository;
  private final AuthenticationFacade authenticationFacade;

  public MeetingCreateResDTO createMeeting(String isbn, MeetingCreateReqDTO request) {
    Book book = bookCommandService.getOrCreateByIsbn(isbn);

    Meeting meeting = MeetingConverter.toEntity(book, request);
    Meeting savedMeeting = meetingRepository.save(meeting);

    Long memberId = authenticationFacade.getCurrentMemberId();
    Member leader = memberRepository.getReferenceById(memberId);
    meetingParticipantRepository.save(MeetingParticipant.createLeader(savedMeeting, leader));
    savedMeeting.addParticipant();

    return MeetingCreateResDTO.from(savedMeeting);
  }

  public MeetingParticipationResDTO participateMeeting(Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));
    Long memberId = authenticationFacade.getCurrentMemberId();

    if (meetingParticipantRepository.existsByMeetingIdAndMemberId(meetingId, memberId)) {
      throw new MeetingException(MeetingErrorCode.DUPLICATE_MEETING);
    }
    if (meeting.getStatus() != MeetingStatus.RECRUITING
        || meeting.getCurParticipants() >= meeting.getMaxParticipants()) {
      throw new MeetingException(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    }

    Member member = memberRepository.getReferenceById(memberId);

    MeetingParticipant meetingParticipant =
        meetingParticipantRepository.save(MeetingParticipant.create(meeting, member));
    meeting.addParticipant();

    return MeetingParticipationResDTO.from(meetingParticipant);
  }

  public boolean deleteUnderstaffedMeeting(Long meetingId, LocalDateTime now) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || meeting.getStatus() != MeetingStatus.RECRUITING
        || meeting.getStartDate().isAfter(now)
        || meeting.getCurParticipants() >= meeting.getMaxParticipants()) {
      return false;
    }

    chatRoomRepository.deleteAllByMeetingId(meetingId);
    meetingParticipantRepository.deleteAllByMeetingId(meetingId);
    meetingRepository.delete(meeting);
    return true;
  }

  // 채팅방 최초 유효 구독 시 출석 처리("1회 이상 입장 = 출석"). 이미 true면 멱등하게 무시하며,
  // 한번 true가 되면 재접속·해제로 되돌리지 않는다. 모임 종료 시 attended != true가 노쇼로 확정된다.
  public void markAttended(Long chatroomId, Long memberId) {
    meetingParticipantRepository.markAttendedByChatroom(chatroomId, memberId);
  }
}
