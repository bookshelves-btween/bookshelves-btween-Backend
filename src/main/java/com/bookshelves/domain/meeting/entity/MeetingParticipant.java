package com.bookshelves.domain.meeting.entity;

import com.bookshelves.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_meeting_participant_meeting_member",
          columnNames = {"meeting_id", "member_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id", nullable = false)
  private Meeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "joined_at", nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime joinedAt;

  @Column(name = "isLeader", nullable = false)
  private Boolean isLeader = false;

  @Column(name = "attended")
  private Boolean attended;
}
