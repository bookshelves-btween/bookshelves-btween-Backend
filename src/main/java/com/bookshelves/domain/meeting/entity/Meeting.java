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

  // 테스트용: 시작 6시간 전 사전 마감 판정을 없앤다. 모집 마감 = 시작 시각이 되므로
  // 지금부터 몇 분 뒤 시작하는 모임을 만들어도 스케줄러가 미리 마감·삭제하지 않는다.
  public static final int RECRUITMENT_CLOSE_HOURS_BEFORE_START = 0;

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

  // 현재 참여자에게 공개된 AI 질문의 question_order. 0 = 아직 공개 전(모임 시작 전).
  // 질문 5개는 모임 시작 전에 미리 저장되므로 "저장된 행 수"가 진행도를 뜻하지 않는다 — 커서로 분리한다.
  // 기존 행에도 값이 필요해 DB 기본값 0을 함께 지정한다(ddl-auto가 컬럼을 추가할 때 적용).
  @Column(
      name = "current_question_order",
      nullable = false,
      columnDefinition = "INT NOT NULL DEFAULT 0")
  private Integer currentQuestionOrder = 0;

  @Builder
  private Meeting(Book book, LocalDateTime startDate, Integer duration, Integer maxParticipants) {
    this.book = book;
    this.startDate = startDate;
    this.duration = duration;
    this.maxParticipants = maxParticipants;
    this.curParticipants = 0;
    this.realParticipants = 0;
    this.status = MeetingStatus.RECRUITING;
    this.currentQuestionOrder = 0;
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

  // 시작과 동시에 1번 질문을 공개한다 — IN_PROGRESS인데 표시할 질문이 없는 구간을 만들지 않는다
  public void start() {
    this.status = MeetingStatus.IN_PROGRESS;
    this.currentQuestionOrder = 1;
  }

  /** 다음 질문을 공개한다. 상한(질문 수) 판단은 호출부가 한다. */
  public void revealNextQuestion() {
    this.currentQuestionOrder++;
  }

  public void closeRecruitment() {
    this.status = MeetingStatus.RECRUIT_CLOSED;
  }

  // 테스트용: 최소 인원 조건을 없앤다. 참여자 수와 무관하게 모임이 성립하므로
  // 인원 미달로 모임이 삭제되는 경로를 타지 않는다.
  public boolean canStart() {
    return true;
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
