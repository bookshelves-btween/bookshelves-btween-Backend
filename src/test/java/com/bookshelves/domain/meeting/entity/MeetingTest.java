package com.bookshelves.domain.meeting.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MeetingTest {

  @Test
  void closesRecruitmentWhenLastParticipantJoins() {
    Meeting meeting =
        Meeting.builder()
            .book(org.mockito.Mockito.mock(Book.class))
            .startDate(LocalDateTime.now().plusDays(1))
            .duration(60)
            .maxParticipants(2)
            .build();

    meeting.addParticipant();
    assertThat(meeting.getCurParticipants()).isEqualTo(1);
    assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.RECRUITING);

    meeting.addParticipant();
    assertThat(meeting.getCurParticipants()).isEqualTo(2);
    assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.RECRUIT_CLOSED);
  }

  @Test
  void startsMeeting() {
    Meeting meeting =
        Meeting.builder()
            .book(org.mockito.Mockito.mock(Book.class))
            .startDate(LocalDateTime.now())
            .duration(60)
            .maxParticipants(1)
            .build();
    meeting.addParticipant();

    meeting.start();

    assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.IN_PROGRESS);
  }

  @Test
  void requiresAtLeastThreeParticipantsToStart() {
    Meeting meeting =
        Meeting.builder()
            .book(org.mockito.Mockito.mock(Book.class))
            .startDate(LocalDateTime.now())
            .duration(60)
            .maxParticipants(4)
            .build();

    meeting.addParticipant();
    meeting.addParticipant();
    assertThat(meeting.canStart()).isFalse();

    meeting.addParticipant();
    assertThat(meeting.canStart()).isTrue();
  }

  @Test
  void calculatesRecruitmentCloseDateSixHoursBeforeStart() {
    LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 20, 0);
    Meeting meeting =
        Meeting.builder()
            .book(org.mockito.Mockito.mock(Book.class))
            .startDate(startDate)
            .duration(60)
            .maxParticipants(4)
            .build();

    assertThat(meeting.getRecruitmentCloseDate()).isEqualTo(startDate.minusHours(6));
  }

  @Test
  void determinesWhetherRecruitmentDeadlineHasPassed() {
    LocalDateTime startDate = LocalDateTime.of(2026, 8, 1, 20, 0);
    Meeting meeting =
        Meeting.builder()
            .book(org.mockito.Mockito.mock(Book.class))
            .startDate(startDate)
            .duration(60)
            .maxParticipants(4)
            .build();

    assertThat(meeting.isRecruitmentClosedAt(startDate.minusHours(6).minusNanos(1))).isFalse();
    assertThat(meeting.isRecruitmentClosedAt(startDate.minusHours(6))).isTrue();
    assertThat(meeting.isRecruitmentClosedAt(startDate.minusHours(5))).isTrue();
  }
}
