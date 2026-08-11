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

  // 오늘의 추천 후보를 고르기 위한 조회. 엔티티가 아니라 ID만 읽어 하루 한 번 도는 스케줄러가
  // 책 전체를 메모리에 올리지 않게 한다.
  //
  // 후보는 문학(KDC 800)과 철학(KDC 100) 두 대분류로 한정한다. kdc_code는 813, 199처럼 세 자리로
  // 저장되므로 첫 자리로 대분류를 가른다. 정보나루에서 KDC를 못 받아온 책은 kdc_code가 null이라
  // like 비교에서 자연히 빠진다.
  @Query("select book.id from Book book where book.kdcCode like '1%' or book.kdcCode like '8%'")
  List<Long> findRecommendableIds();
}
