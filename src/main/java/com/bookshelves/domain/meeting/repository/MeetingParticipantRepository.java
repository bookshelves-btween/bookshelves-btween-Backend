package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

  boolean existsByMeetingIdAndMemberId(Long meetingId, Long memberId);
}
