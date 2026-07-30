package com.bookshelves.domain.book.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookCalendarResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookStatisticsResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookUpsertResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.exception.code.BookSuccessCode;
import com.bookshelves.global.apiPayload.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class BookResponseSerializationTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void bookApiResultsUseResourceBasedObjectNames() {
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.EXTERNAL_BOOK_SEARCHED,
            new BookSearchResDTO(
                List.of(
                    new BookSearchResDTO.BookInfo(
                        "9788936434595", "혼모노", null, null, null, null, null, true)),
                1,
                15,
                false)),
        "{\"books\":[");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.RECENT_BOOK_SEARCHES_FOUND,
            new RecentBookSearchResDTO(
                List.of(new RecentBookSearchResDTO.RecentSearchInfo("혼모노", null)))),
        "{\"recentSearches\":[");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.BOOK_DETAIL_FOUND,
            new BookDetailResDTO(
                new BookDetailResDTO.BookInfo(
                    1L, "9788936434595", "혼모노", null, null, null, null, null, "813", "문학"),
                null)),
        "{\"book\":{");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.MEMBER_BOOK_LIST_FOUND,
            new MemberBookListResDTO(
                List.of(
                    new MemberBookListResDTO.MemberBookInfo(
                        new MemberBookListResDTO.MemberBookRecord(
                            1L, 10, "READING", BigDecimal.ONE, null, null),
                        new MemberBookListResDTO.BookInfo(
                            2L, "9788936434595", "혼모노", null, null, null, "813", "문학"))),
                1,
                20,
                false)),
        "{\"memberBooks\":[{\"memberBook\":{");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.CATEGORY_LIST_FOUND,
            new CategoryListResDTO(List.of(new CategoryListResDTO.CategoryInfo(1L, "800", "문학")))),
        "{\"categories\":[");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.MEMBER_BOOK_CALENDAR_FOUND,
            new MemberBookCalendarResDTO(
                2026, 7, List.of(new MemberBookCalendarResDTO.CalendarDay(null, "cover")))),
        "{\"year\":2026,\"month\":7,\"days\":[");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.MEMBER_BOOK_STATISTICS_FOUND,
            new MemberBookStatisticsResDTO(2026, 7, 1L, 1L, BigDecimal.valueOf(4.0), List.of())),
        "{\"year\":2026,\"month\":7,");
    assertResultStartsWith(
        ApiResponse.onSuccess(
            BookSuccessCode.MEMBER_BOOK_CREATED, MemberBookUpsertResDTO.withHistory(1L)),
        "{\"memberBookHistory\":{");

    String deleteResponse =
        jsonMapper.writeValueAsString(
            ApiResponse.onSuccess(BookSuccessCode.MEMBER_BOOK_DELETED, null));
    assertThat(deleteResponse).contains("\"result\":null");
  }

  private void assertResultStartsWith(ApiResponse<?> response, String expectedResultPrefix) {
    String json = jsonMapper.writeValueAsString(response);

    assertThat(json)
        .contains("\"isSuccess\":true")
        .contains("\"code\":")
        .contains("\"message\":")
        .contains("\"result\":" + expectedResultPrefix);
  }
}
