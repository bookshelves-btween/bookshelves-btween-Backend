package com.bookshelves.domain.meeting.entity;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting extends BaseEntity {

  public static final int MIN_PARTICIPANTS = 3;
  public static final int RECRUITMENT_CLOSE_HOURS_BEFORE_START = 6;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "start_date", nullable = false)
  private LocalDateTime startDate;

  @Column(name = "duration", nullable = false)
  private Integer duration;

  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants;

  @Column(name = "cur_participants", nullable = false)
  private Integer curParticipants;

  @Column(name = "real_participants", nullable = false)
  private Integer realParticipants = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private MeetingStatus status = MeetingStatus.RECRUITING;

  @Builder
  private Meeting(Book book, LocalDateTime startDate, Integer duration, Integer maxParticipants) {
    this.book = book;
    this.startDate = startDate;
    this.duration = duration;
    this.maxParticipants = maxParticipants;
    this.curParticipants = 0;
    this.realParticipants = 0;
    this.status = MeetingStatus.RECRUITING;
  }

  public void addParticipant() {
    this.curParticipants++;
    if (this.curParticipants >= this.maxParticipants) {
      this.status = MeetingStatus.RECRUIT_CLOSED;
    }
  }

  public void complete() {
    this.status = MeetingStatus.COMPLETED;
  }

  public void start() {
    this.status = MeetingStatus.IN_PROGRESS;
  }

  public void closeRecruitment() {
    this.status = MeetingStatus.RECRUIT_CLOSED;
  }

  public boolean canStart() {
    return this.curParticipants >= MIN_PARTICIPANTS;
  }

  public LocalDateTime getRecruitmentCloseDate() {
    return this.startDate.minusHours(RECRUITMENT_CLOSE_HOURS_BEFORE_START);
  }

  public boolean isRecruitmentClosedAt(LocalDateTime now) {
    return !getRecruitmentCloseDate().isAfter(now);
  }

  // 종료 시각 = 시작 시각 + 진행 시간(분)
  public LocalDateTime getEndDate() {
    return this.startDate.plusMinutes(this.duration);
  }
}
