package com.bookshelves.domain.ai.entity;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 클래스명이 AIRecommendation이라 기본 전략으로는 airecommendation이 된다.
// 나머지 테이블이 전부 스네이크케이스이므로 이름을 명시한다.
@Getter
@Entity
@Table(name = "ai_recommendation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIRecommendation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;
}
