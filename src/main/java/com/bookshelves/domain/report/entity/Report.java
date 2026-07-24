package com.bookshelves.domain.report.entity;

import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.report.enums.ReportStatus;
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

@Getter
@Entity
@Table(
    name = "report",
    uniqueConstraints = {
      // 같은 사람이 같은 방을 재신고할 수 없다 — 사전 조회(existsBy)와 저장 사이 경쟁을 DB가 최종 차단
      @UniqueConstraint(
          name = "uk_report_reporter_chatroom",
          columnNames = {"reporter_member_id", "chatroom_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chatroom_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_member_id", nullable = false)
  private Member reporterMember;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReportStatus status = ReportStatus.PENDING;

  @Builder
  private Report(ChatRoom chatRoom, Member reporterMember) {
    this.chatRoom = chatRoom;
    this.reporterMember = reporterMember;
  }
}
