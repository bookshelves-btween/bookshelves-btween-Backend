package com.bookshelves.domain.notification.entity;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.global.entity.BaseEntity;
import com.bookshelves.global.util.TextTruncator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "notification",
    uniqueConstraints = {
      // 대상 ID가 있으면 회원·유형·대상 ID가 같은 알림의 중복 저장을 막는다.
      @UniqueConstraint(
          name = "uk_notification_member_type_related",
          columnNames = {"member_id", "type", "related_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  public static final int MAX_TITLE_LENGTH = 255;

  private static final String MEETING_STARTED_TITLE_SUFFIX = " 독서 모임이 시작되었어요";
  private static final String MEETING_SUMMARY_DONE_TITLE_SUFFIX = " 모임 요약이 준비되었어요";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
  private String title;

  @Column(name = "content", length = 500)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private NotificationType type;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  @Column(name = "related_id")
  private Long relatedId;

  private static final DateTimeFormatter MEETING_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("M/d (E) · HH:mm", Locale.KOREAN);

  public static Notification meetingCanceled(Member member, Meeting meeting) {
    Notification notification = new Notification();
    notification.member = member;
    notification.title = "최소 인원 미달로 모임이 취소되었어요";
    notification.content =
        "%s | %s | %d/%d"
            .formatted(
                meeting.getBook().getTitle(),
                meeting.getStartDate().format(MEETING_DATE_FORMATTER),
                meeting.getCurParticipants(),
                meeting.getMaxParticipants());
    notification.type = NotificationType.MEETING_CANCELED;
    // 삭제되는 모임이므로 이동 대상 ID를 남기지 않는다.
    notification.relatedId = null;
    return notification;
  }

  public static Notification meetingStarted(Member member, Meeting meeting) {
    Notification notification = new Notification();
    notification.member = member;
    notification.title = withBookTitle(meeting.getBook().getTitle(), MEETING_STARTED_TITLE_SUFFIX);
    notification.content = "지금 모임에 참여해보세요";
    notification.type = NotificationType.MEETING_STARTED;
    notification.relatedId = meeting.getId();
    return notification;
  }

  public static Notification meetingSummaryDone(Member member, Meeting meeting) {
    Notification notification = new Notification();
    notification.member = member;
    notification.title =
        withBookTitle(meeting.getBook().getTitle(), MEETING_SUMMARY_DONE_TITLE_SUFFIX);
    notification.content = "모임에서 나눈 이야기를 확인해보세요";
    notification.type = NotificationType.MEETING_SUMMARY_DONE;
    notification.relatedId = meeting.getId();
    return notification;
  }

  private static String withBookTitle(String bookTitle, String suffix) {
    int suffixLength = suffix.codePointCount(0, suffix.length());
    String truncatedBookTitle = TextTruncator.truncate(bookTitle, MAX_TITLE_LENGTH - suffixLength);
    return truncatedBookTitle + suffix;
  }

  public void markAsRead() {
    this.isRead = true;
  }

  public void delete() {
    this.isDeleted = true;
  }
}
