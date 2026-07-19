package com.bookshelves.domain.book.entity;

import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "isbn", nullable = false, unique = true, length = 20)
  private String isbn;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "author")
  private String author;

  @Column(name = "publisher")
  private String publisher;

  @Column(name = "published_date")
  private LocalDate publishedDate;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "cover_image_url", length = 500)
  private String coverImageUrl;

  // 세부 KDC 분류를 직접 보유한다. Category는 선호 장르·통계용 100단위(10개)
  // FK를 걸지 않는다 — KDC는 코드 자체로 부모·자식이 정해진다 (예: 813은 800의 하위).
  @Column(name = "kdc_code", length = 20)
  private String kdcCode;

  @Column(name = "kdc_name", length = 100)
  private String kdcName;
}
