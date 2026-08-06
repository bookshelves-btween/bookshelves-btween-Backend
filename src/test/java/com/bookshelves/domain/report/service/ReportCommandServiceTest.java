package com.bookshelves.domain.report.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.report.code.ReportErrorCode;
import com.bookshelves.domain.report.entity.Report;
import com.bookshelves.domain.report.exception.ReportException;
import com.bookshelves.domain.report.repository.ReportRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceTest {

  @Mock private ReportRepository reportRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingParticipantRepository meetingParticipantRepository;
  @Mock private MemberRepository memberRepository;
  @InjectMocks private ReportCommandService reportCommandService;

  @Test
  void locksMeetingBeforeCreatingReport() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    Member reporter = mock(Member.class);
    given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 2L)).willReturn(true);
    given(meeting.hasStarted()).willReturn(true);
    given(memberRepository.getReferenceById(2L)).willReturn(reporter);
    given(reportRepository.saveAndFlush(any(Report.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    reportCommandService.createReport(10L, 2L);

    InOrder order = inOrder(chatRoomRepository, meetingRepository, reportRepository);
    order.verify(chatRoomRepository).findById(10L);
    order.verify(meetingRepository).findByIdForUpdate(1L);
    order.verify(reportRepository).saveAndFlush(any(Report.class));
  }

  @Test
  void rejectsReportBeforeMeetingStarts() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 2L)).willReturn(true);
    given(meeting.hasStarted()).willReturn(false);

    assertThatThrownBy(() -> reportCommandService.createReport(10L, 2L))
        .isInstanceOf(ReportException.class)
        .extracting("errorCode")
        .isEqualTo(ReportErrorCode.MEETING_NOT_STARTED);

    verify(reportRepository, never()).saveAndFlush(any());
  }

  // 비참여자에게는 그 모임이 시작했는지조차 알려주지 않는다 — 참여자 확인이 상태 확인보다 앞선다.
  @Test
  void hidesMeetingStatusFromNonParticipant() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 2L)).willReturn(false);

    assertThatThrownBy(() -> reportCommandService.createReport(10L, 2L))
        .isInstanceOf(ReportException.class)
        .extracting("errorCode")
        .isEqualTo(ReportErrorCode.NOT_PARTICIPANT);

    verify(meeting, never()).hasStarted();
  }

  // existsBy와 저장 사이의 동시 요청은 유니크 제약이 막는다. 그 위반을 500이 아니라 409로 돌려준다.
  @Test
  void translatesDuplicateKeyViolationIntoConflict() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    Member reporter = mock(Member.class);
    given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(1L, 2L)).willReturn(true);
    given(meeting.hasStarted()).willReturn(true);
    given(reportRepository.existsByReporterMemberIdAndChatRoomId(2L, 10L)).willReturn(false);
    given(memberRepository.getReferenceById(2L)).willReturn(reporter);
    given(reportRepository.saveAndFlush(any(Report.class)))
        .willThrow(new DataIntegrityViolationException("uk_report_reporter_chatroom"));

    assertThatThrownBy(() -> reportCommandService.createReport(10L, 2L))
        .isInstanceOf(ReportException.class)
        .extracting("errorCode")
        .isEqualTo(ReportErrorCode.ALREADY_REPORTED);
  }

  @Test
  void rejectsReportWhenMeetingWasDeletedWhileWaitingForLock() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    given(chatRoomRepository.findById(10L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reportCommandService.createReport(10L, 2L))
        .isInstanceOf(ReportException.class)
        .extracting("errorCode")
        .isEqualTo(ReportErrorCode.CHATROOM_NOT_FOUND);

    verify(meetingParticipantRepository, never()).existsByMeetingIdAndMemberId(any(), any());
    verify(reportRepository, never()).saveAndFlush(any());
  }
}
