package com.bookshelves.domain.ai.entity;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 오늘의 추천 도서. 회원별이 아니라 전역으로 하루 한 권이다.
//
// 클래스명이 AIRecommendation이라 기본 전략으로는 airecommendation이 된다. 나머지 테이블이 전부
// 스네이크케이스이므로 이름을 명시한다.
//
// 제목·저자·출판사·분류·표지는 전부 book_id를 따라간다. 여기에 복제해두면 책 정보가 갱신됐을 때
// 추천 카드만 옛 값을 보여주게 된다.
@Getter
@Entity
@Table(name = "ai_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIRecommendation extends BaseEntity {

  // 멘트 상한. 생성 실패 시 책 소개 첫 문장으로 폴백하는데 그 길이를 우리가 정하지 않으므로 여유를 둔다.
  public static final int MAX_MESSAGE_LENGTH = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "recommendation_message", nullable = false, length = MAX_MESSAGE_LENGTH)
  private String recommendationMessage;

  // 노출 날짜를 created_at에서 유도하지 않는다. 스케줄러가 23시에 미리 만들어 두므로 생성 시각의
  // 날짜와 노출 날짜가 다르다. unique 제약은 스케줄러가 두 번 돌아도 하루에 두 권이 쌓이지 않게 한다.
  @Column(name = "recommended_date", nullable = false, unique = true)
  private LocalDate recommendedDate;

  @Builder
  private AIRecommendation(Book book, String recommendationMessage, LocalDate recommendedDate) {
    this.book = book;
    this.recommendationMessage = recommendationMessage;
    this.recommendedDate = recommendedDate;
  }
}
