package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBook;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {

  Optional<MemberBook> findByMemberIdAndBookId(Long memberId, Long bookId);

  // 별도 조회 이력이 없어 서재 기록의 최근 수정 시각을 기준으로 선택한다.
  @EntityGraph(attributePaths = "book")
  Optional<MemberBook> findFirstByMemberIdOrderByUpdatedAtDescIdDesc(Long memberId);

  Optional<MemberBook> findByMemberIdAndBookIsbn(Long memberId, String isbn);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberId(Long memberId, Pageable pageable);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgress(Long memberId, Integer progress, Pageable pageable);

  @Query(
      value =
          """
          select count(*) as completedBookCount,
                 coalesce(sum(
                   case
                     when regexp_like(memo, '[^[:space:]]') then 1
                     else 0
                   end
                 ), 0) as reviewCount,
                 avg(rating) as averageRating
          from member_book
          where member_id = :memberId
            and progress = :progress
          """,
      nativeQuery = true)
  CumulativeStatistics findCumulativeStatistics(
      @Param("memberId") Long memberId, @Param("progress") Integer progress);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgressBetween(
      Long memberId, Integer startProgress, Integer endProgress, Pageable pageable);

  @Query(
      value =
          """
          select b.kdc_name as categoryName,
                 count(*) as bookCount
          from member_book mb
          join book b on b.id = mb.book_id
          where mb.member_id = :memberId
            and mb.progress = :progress
            and mb.finished_at >= :startAt
            and mb.finished_at < :endAt
            and regexp_like(b.kdc_code, '^[0-9]{3}$')
            and regexp_like(b.kdc_name, '[^[:space:]]')
            and b.kdc_name <> '미분류'
          group by b.kdc_name
          order by bookCount desc, categoryName asc
          """,
      nativeQuery = true)
  List<MonthlyCategoryCount> findMonthlyCategoryCounts(
      @Param("memberId") Long memberId,
      @Param("progress") Integer progress,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt);

  interface CumulativeStatistics {

    long getCompletedBookCount();

    long getReviewCount();

    Double getAverageRating();
  }

  interface MonthlyCategoryCount {

    String getCategoryName();

    long getBookCount();
  }
}
