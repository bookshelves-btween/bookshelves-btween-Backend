package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MeetingCommandService {

  private final BookRepository bookRepository;
  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final MemberRepository memberRepository;
  private final AuthenticationFacade authenticationFacade;

  public MeetingCreateResDTO createMeeting(String isbn, MeetingCreateReqDTO request) {

    Book book =
        bookRepository
            .findByIsbn(isbn)
            .orElseThrow(
                () -> // TODO: 카카오 도서 API에서 조회한 도서 정보를 DB에 저장한 뒤 반환하도록 변경
                new BookException(BookErrorCode.BOOK_NOT_FOUND));

    Meeting meeting = MeetingConverter.toEntity(book, request);
    Meeting savedMeeting = meetingRepository.save(meeting);

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
    if (meeting.getStatus() != MeetingStatus.RECRUITING) {
      throw new MeetingException(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    }

    Member member = memberRepository.getReferenceById(memberId);

    MeetingParticipant meetingParticipant =
        meetingParticipantRepository.save(MeetingParticipant.create(meeting, member));

    return MeetingParticipationResDTO.from(meetingParticipant);
  }
}
