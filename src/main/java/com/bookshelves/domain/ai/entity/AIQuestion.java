package com.bookshelves.domain.ai.entity;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "ai_question",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_ai_question_meeting_question_order",
          columnNames = {"meeting_id", "question_order"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIQuestion extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id", nullable = false)
  private Meeting meeting;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "question_order", nullable = false)
  private Integer questionOrder;
}
