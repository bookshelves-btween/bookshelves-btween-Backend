package com.bookshelves.domain.ai.entity;

import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_meeting_summary_ai_question",
          columnNames = {"ai_question_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSummary extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 모임 요약은 질문(AIQuestion)별 섹션 row의 집합 — 질문당 요약 1개
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ai_question_id", nullable = false)
  private AIQuestion aiQuestion;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;
}
