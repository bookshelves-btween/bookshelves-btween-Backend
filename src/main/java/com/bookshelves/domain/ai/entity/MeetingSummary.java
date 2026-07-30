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

// 모임 요약 한 주제. 모임당 축 세 개가 하나씩, 정확히 3행 존재한다.
//
// content는 비어 있을 수 있다. 해당 축으로 요약할 대화가 없거나 생성에 실패하면 title에 안내 문구만
// 넣고 본문을 비운다. 프론트가 주제 3칸을 항상 그리므로 행 자체는 반드시 3개여야 한다.
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

  // STRING 매핑이 아니면 ordinal 정수로 저장되어 마이그레이션의 ENUM 컬럼과 어긋난다
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
