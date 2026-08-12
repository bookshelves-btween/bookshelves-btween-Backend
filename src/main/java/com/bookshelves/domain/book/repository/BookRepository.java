package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.Book;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

  Optional<Book> findByIsbn(String isbn);

  @Modifying
  @Query(
      value =
          """
          insert into book (
            isbn, title, author, publisher, published_date, description,
            cover_image_url, kdc_code, kdc_name, created_at, updated_at
          ) values (
            :isbn, :title, :author, :publisher, :publishedDate, :description,
            :coverImageUrl, :kdcCode, :kdcName, now(), now()
          )
          on duplicate key update isbn = :isbn
          """,
      nativeQuery = true)
  void upsert(
      @Param("isbn") String isbn,
      @Param("title") String title,
      @Param("author") String author,
      @Param("publisher") String publisher,
      @Param("publishedDate") LocalDate publishedDate,
      @Param("description") String description,
      @Param("coverImageUrl") String coverImageUrl,
      @Param("kdcCode") String kdcCode,
      @Param("kdcName") String kdcName);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select book from Book book where book.isbn = :isbn")
  Optional<Book> findByIsbnForUpdate(@Param("isbn") String isbn);

  // 문학·철학 KDC 도서의 ID만 추천 후보로 조회한다.
  @Query("select book.id from Book book where book.kdcCode like '1%' or book.kdcCode like '8%'")
  List<Long> findRecommendableIds();
}
