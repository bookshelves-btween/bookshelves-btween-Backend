package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {

  // 홈 응답에 필요한 book을 함께 조회한다.
  @EntityGraph(attributePaths = "book")
  Optional<AIRecommendation> findByRecommendedDate(LocalDate recommendedDate);

  // 오늘 추천이 없으면 가장 최근 추천을 사용한다.
  @EntityGraph(attributePaths = "book")
  Optional<AIRecommendation> findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(
      LocalDate recommendedDate);

  boolean existsByRecommendedDate(LocalDate recommendedDate);

  // 최근 추천 도서를 후보에서 제외하기 위한 조회.
  @Query("select r.book.id from AIRecommendation r where r.recommendedDate >= :since")
  List<Long> findBookIdsRecommendedSince(@Param("since") LocalDate since);
}
