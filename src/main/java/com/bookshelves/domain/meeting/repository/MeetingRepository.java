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

  // 홈의 모집중 모임. 목록 조회와 달리 지금 참여하기를 누를 수 있는 것만 남긴다.
  //
  // 상태만 보면 정원이 찼거나 모집이 끝난 모임까지 섞인다. 홈은 카드마다 참여하기 버튼을 그리므로
  // 눌렀을 때 거절될 모임을 애초에 내려보내지 않는다.
  //
  // earliestStartDate에는 현재 시각이 아니라 모집 마감 기준을 넘긴다. 참여는 시작 6시간 전부터
  // 거절되는데(MeetingCommandService.participateInMeeting), 상태를 바꾸는 배치가 60초 주기라
  // 그 사이 모임은 RECRUITING인 채로 남는다. 시작 시각만 보면 이 구간이 그대로 새어 나온다.
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

  // 종료 배치 대상 후보 — 종료 시각(startDate+duration분)은 DB 종속 함수를 피해 Java에서 필터한다.
  // 진행 중 모임은 "현재 열려 있는 것"뿐이라 수가 적어 전량 로딩해도 부담이 없다.
  List<Meeting> findAllByStatus(MeetingStatus status);

  List<Meeting> findAllByStatusAndStartDateLessThanEqual(
      MeetingStatus status, LocalDateTime startDate);

  List<Meeting> findAllByStatusInAndStartDateLessThanEqual(
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
