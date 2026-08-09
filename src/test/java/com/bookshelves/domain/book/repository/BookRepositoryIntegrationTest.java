package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
class BookRepositoryIntegrationTest {

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

  @Autowired private BookRepository bookRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void limitsRecommendationCandidatesToLiteratureAndPhilosophy() {
    insertBook(301L, "100"); // 철학 대분류 자체
    insertBook(302L, "199"); // 철학 하위
    insertBook(303L, "813"); // 문학 하위
    insertBook(304L, "899"); // 문학 하위
    insertBook(305L, "000"); // 총류
    insertBook(306L, "900"); // 역사
    insertBook(307L, null); // KDC를 못 받아온 책

    assertThat(bookRepository.findRecommendableIds())
        .containsExactlyInAnyOrder(301L, 302L, 303L, 304L);
  }

  private void insertBook(long bookId, String kdcCode) {
    jdbcTemplate.update(
        """
        insert into book (id, isbn, title, kdc_code, created_at, updated_at)
        values (?, ?, ?, ?, now(6), now(6))
        """,
        bookId,
        "978000000" + bookId,
        "Book " + bookId,
        kdcCode);
  }
}
