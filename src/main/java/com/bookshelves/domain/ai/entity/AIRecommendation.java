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

// 모든 회원에게 공통으로 노출되는 하루 한 권의 추천 도서.
@Getter
@Entity
@Table(name = "ai_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIRecommendation extends BaseEntity {

  // 책 소개를 사용하는 폴백까지 수용할 수 있는 길이.
  public static final int MAX_MESSAGE_LENGTH = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "recommendation_message", nullable = false, length = MAX_MESSAGE_LENGTH)
  private String recommendationMessage;

  // 전날 미리 생성되므로 생성 시각과 노출 날짜를 분리한다.
  @Column(name = "recommended_date", nullable = false, unique = true)
  private LocalDate recommendedDate;

  @Builder
  private AIRecommendation(Book book, String recommendationMessage, LocalDate recommendedDate) {
    this.book = book;
    this.recommendationMessage = recommendationMessage;
    this.recommendedDate = recommendedDate;
  }
}
