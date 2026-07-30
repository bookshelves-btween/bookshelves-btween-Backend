package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBook;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {

  Optional<MemberBook> findByMemberIdAndBookId(Long memberId, Long bookId);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberId(Long memberId, Pageable pageable);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgress(Long memberId, Integer progress, Pageable pageable);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgressBetween(
      Long memberId, Integer startProgress, Integer endProgress, Pageable pageable);

  @Query(
      """
      select
        count(case when memberBook.progress = 100 then 1 end) as completedBookCount,
        count(case when memberBook.memo is not null and trim(memberBook.memo) <> '' then 1 end)
            as reviewCount,
        avg(memberBook.rating) as averageRating
      from MemberBook memberBook
      where memberBook.member.id = :memberId
      """)
  MemberBookStatistics findStatisticsByMemberId(@Param("memberId") Long memberId);

  interface MemberBookStatistics {

    long getCompletedBookCount();

    long getReviewCount();

    Double getAverageRating();
  }
}
