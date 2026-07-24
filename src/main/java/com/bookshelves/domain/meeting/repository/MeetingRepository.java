package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  String SEARCHABLE_MEETINGS_FROM_WHERE =
      """
      from Meeting meeting
      where meeting.status = :status
        and lower(meeting.book.title) like lower(concat('%', :title, '%'))
        and not exists (
          select participant.id
          from MeetingParticipant participant
          where participant.meeting = meeting
            and participant.member.id = :memberId
        )
      """;

  @EntityGraph(attributePaths = "book")
  Optional<Meeting> findWithBookById(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select meeting from Meeting meeting where meeting.id = :id")
  Optional<Meeting> findByIdForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = "book")
  @Query(
      value = "select meeting\n" + SEARCHABLE_MEETINGS_FROM_WHERE,
      countQuery = "select count(meeting)\n" + SEARCHABLE_MEETINGS_FROM_WHERE)
  Page<Meeting> findSearchableMeetings(
      @Param("title") String title,
      @Param("status") MeetingStatus status,
      @Param("memberId") Long memberId,
      Pageable pageable);
}
