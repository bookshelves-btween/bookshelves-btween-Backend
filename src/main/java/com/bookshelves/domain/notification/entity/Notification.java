package com.bookshelves.domain.notification.entity;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.global.entity.BaseEntity;
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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", length = 500)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private NotificationType type;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

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
    notification.relatedId = meeting.getId();
    return notification;
  }

  public static Notification meetingStarted(Member member, Meeting meeting) {
    Notification notification = new Notification();
    notification.member = member;
    notification.title = "%s 독서 모임이 시작되었어요".formatted(meeting.getBook().getTitle());
    notification.content = "지금 모임에 참여해보세요";
    notification.type = NotificationType.MEETING_STARTED;
    notification.relatedId = meeting.getId();
    return notification;
  }

  public void markAsRead() {
    this.isRead = true;
  }
}
