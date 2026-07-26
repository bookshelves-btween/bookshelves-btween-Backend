package com.bookshelves.domain.meeting.entity;

import com.bookshelves.domain.member.entity.Member;
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
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_meeting_participant_meeting_member",
          columnNames = {"meeting_id", "member_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingParticipant extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "meeting_id", nullable = false)
  private Meeting meeting;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "is_leader", nullable = false)
  private Boolean isLeader = false;

  @Column(name = "attended")
  private Boolean attended;

  public static MeetingParticipant create(Meeting meeting, Member member) {
    MeetingParticipant meetingParticipant = new MeetingParticipant();
    meetingParticipant.meeting = meeting;
    meetingParticipant.member = member;
    meetingParticipant.isLeader = false;
    return meetingParticipant;
  }

  public static MeetingParticipant createLeader(Meeting meeting, Member member) {
    MeetingParticipant meetingParticipant = new MeetingParticipant();
    meetingParticipant.meeting = meeting;
    meetingParticipant.member = member;
    meetingParticipant.isLeader = true;
    return meetingParticipant;
  }
}
