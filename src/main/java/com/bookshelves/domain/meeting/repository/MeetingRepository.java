package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  @EntityGraph(attributePaths = "book")
  Optional<Meeting> findWithBookById(Long id);

  @EntityGraph(attributePaths = "book")
  @Query(
      value =
          """
          select meeting
          from Meeting meeting
          where meeting.status = :status
            and lower(meeting.book.title) like lower(concat('%', :title, '%'))
            and not exists (
              select participant.id
              from MeetingParticipant participant
              where participant.meeting = meeting
                and participant.member.id = :memberId
            )
          """,
      countQuery =
          """
          select count(meeting)
          from Meeting meeting
          where meeting.status = :status
            and lower(meeting.book.title) like lower(concat('%', :title, '%'))
            and not exists (
              select participant.id
              from MeetingParticipant participant
              where participant.meeting = meeting
                and participant.member.id = :memberId
            )
          """)
  Page<Meeting> findSearchableMeetings(
      @Param("title") String title,
      @Param("status") MeetingStatus status,
      @Param("memberId") Long memberId,
      Pageable pageable);
}
