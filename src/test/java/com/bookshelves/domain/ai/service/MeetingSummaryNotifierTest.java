package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MeetingSummaryNotifierTest {

  private static final Long MEETING_ID = 1L;

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingParticipantRepository meetingParticipantRepository;
  @Mock private NotificationRepository notificationRepository;

  private MeetingSummaryNotifier notifier;

  @BeforeEach
  void setUp() {
    notifier =
        new MeetingSummaryNotifier(
            meetingRepository, meetingParticipantRepository, notificationRepository);
  }

  private Member member(Long id) {
    Member member = Member.createSocialMember(Provider.KAKAO, "provider-" + id);
    ReflectionTestUtils.setField(member, "id", id);
    return member;
  }

  private Meeting meeting() {
    Meeting meeting =
        Meeting.builder()
            .book(Book.builder().isbn("9788936434595").title("아몬드").build())
            .duration(60)
            .maxParticipants(6)
            .build();
    ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
    return meeting;
  }

  private List<Notification> captureSaved() {
    ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.captor();
    verify(notificationRepository).saveAll(captor.capture());
    return captor.getValue();
  }

  @Test
  void createsNotificationForEveryParticipant() {
    Meeting meeting = meeting();
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(MEETING_ID))
        .willReturn(
            List.of(
                MeetingParticipant.create(meeting, member(10L)),
                MeetingParticipant.create(meeting, member(11L))));
    given(
            notificationRepository.existsByMember_IdAndTypeAndRelatedId(
                any(), eq(NotificationType.MEETING_SUMMARY_DONE), eq(MEETING_ID)))
        .willReturn(false);

    notifier.notifySummaryDone(MEETING_ID);

    List<Notification> saved = captureSaved();
    assertThat(saved).hasSize(2);
    assertThat(saved)
        .allSatisfy(
            notification -> {
              assertThat(notification.getType()).isEqualTo(NotificationType.MEETING_SUMMARY_DONE);
              // 알림을 눌렀을 때 요약 화면으로 이동하려면 모임 ID가 실려야 한다
              assertThat(notification.getRelatedId()).isEqualTo(MEETING_ID);
              assertThat(notification.getTitle()).contains("아몬드");
            });
  }

  @Test
  void skipsMemberWhoAlreadyHasNotification() {
    Meeting meeting = meeting();
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(MEETING_ID))
        .willReturn(
            List.of(
                MeetingParticipant.create(meeting, member(10L)),
                MeetingParticipant.create(meeting, member(11L))));
    given(
            notificationRepository.existsByMember_IdAndTypeAndRelatedId(
                eq(10L), eq(NotificationType.MEETING_SUMMARY_DONE), eq(MEETING_ID)))
        .willReturn(true);
    given(
            notificationRepository.existsByMember_IdAndTypeAndRelatedId(
                eq(11L), eq(NotificationType.MEETING_SUMMARY_DONE), eq(MEETING_ID)))
        .willReturn(false);

    notifier.notifySummaryDone(MEETING_ID);

    // 중복 이벤트로 준비가 두 번 돌아도 같은 회원에게 알림이 두 번 쌓이지 않아야 한다
    assertThat(captureSaved()).hasSize(1);
  }

  @Test
  void savesNothingWhenEveryParticipantAlreadyNotified() {
    Meeting meeting = meeting();
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.findAllWithMemberByMeetingId(MEETING_ID))
        .willReturn(List.of(MeetingParticipant.create(meeting, member(10L))));
    given(notificationRepository.existsByMember_IdAndTypeAndRelatedId(any(), any(), eq(MEETING_ID)))
        .willReturn(true);

    notifier.notifySummaryDone(MEETING_ID);

    verify(notificationRepository, never()).saveAll(any());
  }

  @Test
  void doesNothingWhenMeetingIsAlreadyDeleted() {
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.empty());

    notifier.notifySummaryDone(MEETING_ID);

    verify(notificationRepository, never()).saveAll(any());
  }
}
