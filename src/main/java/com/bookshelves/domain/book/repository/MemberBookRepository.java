package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBook;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {

  Optional<MemberBook> findByMemberIdAndBookId(Long memberId, Long bookId);

  // 홈의 최근 본 책. 별도 조회 이력 테이블이 없어 기록을 마지막으로 건드린 순서를 최근으로 본다.
  // 내 서재 목록도 같은 기준(updatedAt desc)으로 정렬하므로 사용자가 보는 순서와 어긋나지 않는다.
  @EntityGraph(attributePaths = "book")
  Optional<MemberBook> findFirstByMemberIdOrderByUpdatedAtDescIdDesc(Long memberId);

  Optional<MemberBook> findByMemberIdAndBookIsbn(Long memberId, String isbn);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberId(Long memberId, Pageable pageable);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgress(Long memberId, Integer progress, Pageable pageable);

  List<MemberBook> findByMemberIdAndProgress(Long memberId, Integer progress);

  @EntityGraph(attributePaths = "book")
  Page<MemberBook> findByMemberIdAndProgressBetween(
      Long memberId, Integer startProgress, Integer endProgress, Pageable pageable);

  @EntityGraph(attributePaths = "book")
  List<MemberBook> findByMemberIdAndProgressAndFinishedAtGreaterThanEqualAndFinishedAtLessThan(
      Long memberId, Integer progress, LocalDateTime startAt, LocalDateTime endAt);
}
