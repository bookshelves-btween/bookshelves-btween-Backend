package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

  boolean existsByMeetingIdAndMemberId(Long meetingId, Long memberId);

  int countByMeetingId(Long meetingId);

  void deleteAllByMeetingId(Long meetingId);

  @Query(
      "select mp from MeetingParticipant mp join fetch mp.member where mp.meeting.id = :meetingId")
  List<MeetingParticipant> findAllWithMemberByMeetingId(@Param("meetingId") Long meetingId);

  // 노쇼 확정 대상을 회원과 함께 조회한다.
  @Query(
      "select mp from MeetingParticipant mp join fetch mp.member "
          + "where mp.meeting.id = :meetingId "
          + "and (mp.attended is null or mp.attended = false)")
  List<MeetingParticipant> findNotAttendedByMeetingId(@Param("meetingId") Long meetingId);

  // 최초 유효 구독을 출석으로 기록하며 이미 출석한 경우 갱신하지 않는다.
  @Modifying(clearAutomatically = true)
  @Query(
      "update MeetingParticipant mp set mp.attended = true "
          + "where mp.member.id = :memberId "
          + "and mp.meeting.id = "
          + "(select cr.meeting.id from ChatRoom cr where cr.id = :chatroomId) "
          + "and (mp.attended is null or mp.attended = false)")
  int markAttendedByChatroom(
      @Param("chatroomId") Long chatroomId, @Param("memberId") Long memberId);
}
