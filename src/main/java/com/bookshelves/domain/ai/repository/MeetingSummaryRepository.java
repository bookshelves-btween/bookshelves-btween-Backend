package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {

  @Query(
      """
      select meetingSummary
      from MeetingSummary meetingSummary
      join fetch meetingSummary.aiQuestion aiQuestion
      where aiQuestion.meeting.id = :meetingId
      order by aiQuestion.questionOrder
      """)
  List<MeetingSummary> findAllByMeetingIdOrderByQuestionOrder(@Param("meetingId") Long meetingId);
}
