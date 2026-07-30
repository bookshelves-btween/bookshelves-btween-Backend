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

  // 홈은 추천 도서의 제목·저자·표지를 함께 그린다. book을 지연 로딩으로 두면 조회마다 쿼리가 하나 더 나간다.
  @EntityGraph(attributePaths = "book")
  Optional<AIRecommendation> findByRecommendedDate(LocalDate recommendedDate);

  // 오늘치가 없을 때 쓰는 폴백. 스케줄러가 도는 23시에 서버가 내려가 있었으면 오늘 행이 비는데,
  // 그때 홈에서 추천 영역을 통째로 지우는 것보다 어제 책을 하루 더 보여주는 편이 낫다.
  @EntityGraph(attributePaths = "book")
  Optional<AIRecommendation> findFirstByRecommendedDateLessThanEqualOrderByRecommendedDateDesc(
      LocalDate recommendedDate);

  boolean existsByRecommendedDate(LocalDate recommendedDate);

  // 최근 추천된 책을 후보에서 빼기 위한 조회. 책이 제외 기간보다 적으면 후보가 비는데,
  // 그 처리는 호출부가 맡는다.
  @Query("select r.book.id from AIRecommendation r where r.recommendedDate >= :since")
  List<Long> findBookIdsRecommendedSince(@Param("since") LocalDate since);
}
