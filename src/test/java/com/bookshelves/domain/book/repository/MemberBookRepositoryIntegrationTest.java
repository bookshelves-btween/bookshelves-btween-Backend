package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberBookRepositoryIntegrationTest {

  @Container
  static final MySQLContainer MYSQL =
      new MySQLContainer("mysql:8.4")
          .withDatabaseName("bookshelves_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private MemberBookRepository memberBookRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void aggregatesCompletedBooksWithMySqlWhitespaceSemantics() {
    insertMember(1L);
    insertMemberBook(1L, 101L, 100, null, null);
    insertMemberBook(1L, 102L, 100, "", new BigDecimal("3.0"));
    insertMemberBook(1L, 103L, 100, "   ", null);
    insertMemberBook(1L, 104L, 100, "\t\r\n", new BigDecimal("2.0"));
    insertMemberBook(1L, 105L, 100, "review", new BigDecimal("4.0"));
    insertMemberBook(1L, 106L, 100, " \t review \n", null);
    insertMemberBook(1L, 107L, 50, "not completed", new BigDecimal("5.0"));

    MemberBookRepository.CumulativeStatistics result =
        memberBookRepository.findCumulativeStatistics(1L, 100);

    assertThat(result.getCompletedBookCount()).isEqualTo(6L);
    assertThat(result.getReviewCount()).isEqualTo(2L);
    assertThat(result.getAverageRating()).isEqualTo(3.0);
  }

  @Test
  void returnsNullAverageWhenCompletedBooksHaveNoRatings() {
    insertMember(2L);
    insertMemberBook(2L, 201L, 100, null, null);
    insertMemberBook(2L, 202L, 100, "review", null);

    MemberBookRepository.CumulativeStatistics result =
        memberBookRepository.findCumulativeStatistics(2L, 100);

    assertThat(result.getCompletedBookCount()).isEqualTo(2L);
    assertThat(result.getReviewCount()).isEqualTo(1L);
    assertThat(result.getAverageRating()).isNull();
  }

  private void insertMember(long memberId) {
    jdbcTemplate.update(
        """
        insert into member (id, status, created_at, updated_at)
        values (?, 'ACTIVE', now(6), now(6))
        """,
        memberId);
  }

  private void insertMemberBook(
      long memberId, long bookId, int progress, String memo, BigDecimal rating) {
    jdbcTemplate.update(
        """
        insert into book (id, isbn, title, created_at, updated_at)
        values (?, ?, ?, now(6), now(6))
        """,
        bookId,
        "978000000" + bookId,
        "Book " + bookId);
    jdbcTemplate.update(
        """
        insert into member_book
          (progress, rating, book_id, member_id, memo, created_at, updated_at)
        values (?, ?, ?, ?, ?, now(6), now(6))
        """,
        progress,
        rating,
        bookId,
        memberId,
        memo);
  }
}
