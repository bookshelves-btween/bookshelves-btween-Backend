package com.bookshelves.domain.ai.entity;

import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.meeting.entity.Meeting;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 모임별로 세 가지 축에 하나씩 저장되는 요약.
// 요약할 내용이 없으면 안내 제목만 저장하고 content는 비워 둔다.
@Getter
@Entity
@Table(
    name = "meeting_summary",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_meeting_summary_meeting_axis",
          columnNames = {"meeting_id", "axis"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingSummary extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id", nullable = false)
  private Meeting meeting;

  @Enumerated(EnumType.STRING)
  @Column(name = "axis", nullable = false)
  private SummaryAxis axis;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Builder
  private MeetingSummary(Meeting meeting, SummaryAxis axis, String title, String content) {
    this.meeting = meeting;
    this.axis = axis;
    this.title = title;
    this.content = content;
  }
}
