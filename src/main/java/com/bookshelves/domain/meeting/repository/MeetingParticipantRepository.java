package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

  boolean existsByMeetingIdAndMemberId(Long meetingId, Long memberId);

  int countByMeetingId(Long meetingId);

  // 채팅방 최초 유효 구독 = 출석. 이미 true면 갱신하지 않아 멱등하며, 한번 true가 되면
  // 재접속·해제로 되돌리지 않는다. chatroomId로 대상 모임을 특정한다.
  @Modifying(clearAutomatically = true)
  @Query(
      "update MeetingParticipant mp set mp.attended = true "
          + "where mp.member.id = :memberId "
          + "and mp.meeting.id = (select cr.meeting.id from ChatRoom cr where cr.id = :chatroomId) "
          + "and (mp.attended is null or mp.attended = false)")
  int markAttendedByChatroom(
      @Param("chatroomId") Long chatroomId, @Param("memberId") Long memberId);
}
