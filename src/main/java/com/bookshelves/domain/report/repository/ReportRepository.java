package com.bookshelves.domain.report.repository;

import com.bookshelves.domain.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByReporterMemberIdAndChatRoomId(Long reporterMemberId, Long chatRoomId);

  @Modifying
  @Query(
      """
      delete from Report report
      where report.chatRoom.id in (
        select chatRoom.id from ChatRoom chatRoom where chatRoom.meeting.id = :meetingId
      )
      """)
  void deleteAllByMeetingId(@Param("meetingId") Long meetingId);
}
