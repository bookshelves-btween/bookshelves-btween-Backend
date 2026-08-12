package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
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

  // 홈에는 정원과 모집 기한 조건까지 충족해 즉시 참여할 수 있는 모임만 노출한다.
  // 배치 전 상태 지연을 보완하기 위해 earliestStartDate에는 모집 마감 기준을 전달한다.
  String JOINABLE_MEETINGS_FROM_WHERE =
      """
      from Meeting meeting
      where meeting.status = :status
        and meeting.startDate > :earliestStartDate
        and meeting.curParticipants < meeting.maxParticipants
        and not exists (
          select participant.id
          from MeetingParticipant participant
          where participant.meeting = meeting
            and participant.member.id = :memberId
        )
      order by meeting.startDate asc, meeting.id asc
      """;

  @EntityGraph(attributePaths = "book")
  Optional<Meeting> findWithBookById(Long id);

  @EntityGraph(attributePaths = "book")
  @Query("select meeting\n" + JOINABLE_MEETINGS_FROM_WHERE)
  List<Meeting> findJoinableMeetings(
      @Param("status") MeetingStatus status,
      @Param("earliestStartDate") LocalDateTime earliestStartDate,
      @Param("memberId") Long memberId,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select meeting from Meeting meeting where meeting.id = :id")
  Optional<Meeting> findByIdForUpdate(@Param("id") Long id);

  // DB 종속 날짜 함수를 피하기 위해 종료 시각은 서비스에서 계산한다.
  List<Meeting> findAllByStatus(MeetingStatus status);

  List<Meeting> findAllByStatusAndStartDateLessThanEqual(
      MeetingStatus status, LocalDateTime startDate);

  List<Meeting> findAllByStatusInAndStartDateLessThanEqual(
      List<MeetingStatus> statuses, LocalDateTime startDate);

  List<Meeting> findAllByStatusInAndStartDateAfter(
      List<MeetingStatus> statuses, LocalDateTime startDate);

  @EntityGraph(attributePaths = "book")
  @Query(
      value = "select meeting\n" + SEARCHABLE_MEETINGS_FROM_WHERE,
      countQuery = "select count(meeting)\n" + SEARCHABLE_MEETINGS_FROM_WHERE)
  Page<Meeting> findSearchableMeetings(
      @Param("title") String title,
      @Param("status") MeetingStatus status,
      @Param("memberId") Long memberId,
      Pageable pageable);

  @Query(
      value =
          """
          select meeting
          from MeetingParticipant participant
          join participant.meeting meeting
          join fetch meeting.book
          where participant.member.id = :memberId
            and participant.isLeader = :isLeader
            and (:year is null or year(meeting.startDate) = :year)
            and (:month is null or month(meeting.startDate) = :month)
          order by meeting.startDate asc, meeting.id asc
          """,
      countQuery =
          """
          select count(participant.meeting)
          from MeetingParticipant participant
          where participant.member.id = :memberId
            and participant.isLeader = :isLeader
            and (:year is null or year(participant.meeting.startDate) = :year)
            and (:month is null or month(participant.meeting.startDate) = :month)
          """)
  Page<Meeting> findMyMeetings(
      @Param("memberId") Long memberId,
      @Param("isLeader") boolean isLeader,
      @Param("year") Integer year,
      @Param("month") Integer month,
      Pageable pageable);
}
