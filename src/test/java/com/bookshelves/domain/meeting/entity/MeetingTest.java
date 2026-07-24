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
}
