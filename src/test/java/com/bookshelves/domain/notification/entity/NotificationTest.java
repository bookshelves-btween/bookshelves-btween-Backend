package com.bookshelves.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  void markAsReadIsIdempotent() {
    Book book = Book.builder().isbn("9788936434595").title("아몬드").build();
    Meeting meeting =
        Meeting.builder()
            .book(book)
            .startDate(LocalDateTime.of(2026, 8, 6, 20, 0))
            .duration(60)
            .maxParticipants(4)
            .build();
    Notification notification = Notification.meetingStarted(mock(Member.class), meeting);

    notification.markAsRead();
    notification.markAsRead();

    assertThat(notification.getIsRead()).isTrue();
  }

  @Test
  void preservesSuffixWhenBookTitleExceedsNotificationTitleLimit() {
    Book book = Book.builder().isbn("9788936434595").title("😀".repeat(255)).build();
    Meeting meeting =
        Meeting.builder()
            .book(book)
            .startDate(LocalDateTime.of(2026, 8, 6, 20, 0))
            .duration(60)
            .maxParticipants(4)
            .build();
    Member member = mock(Member.class);

    Notification started = Notification.meetingStarted(member, meeting);
    Notification summaryDone = Notification.meetingSummaryDone(member, meeting);

    assertThat(started.getTitle()).endsWith(" 독서 모임이 시작되었어요");
    assertThat(summaryDone.getTitle()).endsWith(" 모임 요약이 준비되었어요");
    assertThat(started.getTitle().codePointCount(0, started.getTitle().length()))
        .isEqualTo(Notification.MAX_TITLE_LENGTH);
    assertThat(summaryDone.getTitle().codePointCount(0, summaryDone.getTitle().length()))
        .isEqualTo(Notification.MAX_TITLE_LENGTH);
  }
}
