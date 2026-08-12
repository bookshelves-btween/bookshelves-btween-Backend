package com.bookshelves.domain.book.entity;

import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

  public static final int MAX_TITLE_LENGTH = 255;
  public static final int MAX_AUTHOR_LENGTH = 255;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "isbn", nullable = false, unique = true, length = 20)
  private String isbn;

  @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
  private String title;

  @Column(name = "author", length = MAX_AUTHOR_LENGTH)
  private String author;

  @Column(name = "publisher")
  private String publisher;

  @Column(name = "published_date")
  private LocalDate publishedDate;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "cover_image_url", length = 500)
  private String coverImageUrl;

  // 세부 KDC 코드는 자체 계층을 가지므로 Category FK로 연결하지 않는다.
  @Column(name = "kdc_code", length = 20)
  private String kdcCode;

  @Column(name = "kdc_name", length = 100)
  private String kdcName;

  @Builder
  private Book(
      String isbn,
      String title,
      String author,
      String publisher,
      LocalDate publishedDate,
      String description,
      String coverImageUrl,
      String kdcCode,
      String kdcName) {
    this.isbn = isbn;
    this.title = title;
    this.author = author;
    this.publisher = publisher;
    this.publishedDate = publishedDate;
    this.description = description;
    this.coverImageUrl = coverImageUrl;
    this.kdcCode = kdcCode;
    this.kdcName = kdcName;
  }
}
