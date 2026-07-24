package com.bookshelves.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.book.repository.BookRepository;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingCommandServiceTest {

  @Mock private BookRepository bookRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingParticipantRepository meetingParticipantRepository;
  @Mock private MemberRepository memberRepository;
  @Mock private AuthenticationFacade authenticationFacade;
  @InjectMocks private MeetingCommandService meetingCommandService;

  @Test
  void createsMeetingParticipant() {
    Meeting meeting = mock(Meeting.class);
    Member member = mock(Member.class);
    MeetingParticipant savedParticipant = mock(MeetingParticipant.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(memberRepository.getReferenceById(10L)).willReturn(member);
    given(meetingParticipantRepository.save(org.mockito.ArgumentMatchers.any()))
        .willReturn(savedParticipant);
    given(savedParticipant.getId()).willReturn(100L);

    MeetingParticipationResDTO response = meetingCommandService.participateMeeting(1L);

    assertThat(response.meetingParticipantId()).isEqualTo(100L);
    verify(meetingParticipantRepository).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsUnknownMeeting() {
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_NOT_FOUND);
  }

  @Test
  void rejectsDuplicateMeeting() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 10L)).willReturn(true);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.DUPLICATE_MEETING);
    verify(meetingParticipantRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMeetingThatIsNotRecruiting() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(authenticationFacade.getCurrentMemberId()).willReturn(10L);
    given(meeting.getStatus()).willReturn(MeetingStatus.IN_PROGRESS);

    assertThatThrownBy(() -> meetingCommandService.participateMeeting(1L))
        .isInstanceOf(MeetingException.class)
        .extracting("errorCode")
        .isEqualTo(MeetingErrorCode.MEETING_RECRUITMENT_CLOSED);
    verify(meetingParticipantRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
