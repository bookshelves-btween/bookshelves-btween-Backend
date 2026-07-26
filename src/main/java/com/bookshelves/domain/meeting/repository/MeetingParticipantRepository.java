package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

  boolean existsByMeetingIdAndMemberId(Long meetingId, Long memberId);

  int countByMeetingId(Long meetingId);

  // 노쇼 확정 대상 — 출석하지 않은(attended가 true가 아닌) 참여자. member를 함께 로딩한다.
  @Query(
      "select mp from MeetingParticipant mp join fetch mp.member "
          + "where mp.meeting.id = :meetingId and (mp.attended is null or mp.attended = false)")
  List<MeetingParticipant> findNotAttendedByMeetingId(@Param("meetingId") Long meetingId);
}
