package com.bookshelves.domain.book.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.book.dto.request.MemberBookUpsertReqDTO;
import com.bookshelves.domain.book.dto.response.MemberBookUpsertResDTO;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.book.service.BookQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BookControllerTest {

  private static final String ISBN = "9788936434595";

  private final BookQueryService bookQueryService = mock(BookQueryService.class);
  private final BookCommandService bookCommandService = mock(BookCommandService.class);
  private final BookController bookController =
      new BookController(bookQueryService, bookCommandService);

  @Test
  void upsertMemberBookReturnsCreatedWhenMemberBookIsNew() {
    MemberBookUpsertReqDTO request = new MemberBookUpsertReqDTO(30, new BigDecimal("4.5"), "한줄평");
    MemberBookUpsertResDTO result = MemberBookUpsertResDTO.withHistory(10L);
    when(bookCommandService.upsertMemberBook(ISBN, request))
        .thenReturn(new BookCommandService.MemberBookUpsertResult(true, result));

    ResponseEntity<ApiResponse<MemberBookUpsertResDTO>> response =
        bookController.upsertMemberBook(ISBN, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BOOK201_1");
    assertThat(response.getBody().getResult()).isSameAs(result);
  }

  @Test
  void upsertMemberBookReturnsOkWhenMemberBookAlreadyExists() {
    MemberBookUpsertReqDTO request =
        new MemberBookUpsertReqDTO(60, new BigDecimal("4.5"), "수정 한줄평");
    MemberBookUpsertResDTO result = MemberBookUpsertResDTO.withHistory(11L);
    when(bookCommandService.upsertMemberBook(ISBN, request))
        .thenReturn(new BookCommandService.MemberBookUpsertResult(false, result));

    ResponseEntity<ApiResponse<MemberBookUpsertResDTO>> response =
        bookController.upsertMemberBook(ISBN, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("BOOK200_5");
    assertThat(response.getBody().getResult()).isSameAs(result);
  }
}
