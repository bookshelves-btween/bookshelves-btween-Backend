package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBookHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBookHistoryRepository extends JpaRepository<MemberBookHistory, Long> {

  @EntityGraph(attributePaths = "memberBook.book")
  List<MemberBookHistory>
      findByMemberBookMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
          Long memberId, LocalDateTime startAt, LocalDateTime endAt);
}
