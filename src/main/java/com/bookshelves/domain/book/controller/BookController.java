package com.bookshelves.domain.book.controller;

import com.bookshelves.domain.book.dto.request.MemberBookUpsertReqDTO;
import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookUpsertResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.exception.code.BookSuccessCode;
import com.bookshelves.domain.book.service.BookCommandService;
import com.bookshelves.domain.book.service.BookQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookController implements BookControllerDocs {

  private final BookQueryService bookQueryService;
  private final BookCommandService bookCommandService;

  @Override
  @GetMapping("/api/v1/categories")
  public ResponseEntity<ApiResponse<CategoryListResDTO>> getCategories() {
    CategoryListResDTO response = bookQueryService.getCategories();
    return ResponseEntity.ok(ApiResponse.onSuccess(BookSuccessCode.CATEGORY_LIST_FOUND, response));
  }

  @Override
  @GetMapping("/api/v1/books/search")
  public ResponseEntity<ApiResponse<BookSearchResDTO>> searchExternalBooks(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "1") String page,
      @RequestParam(defaultValue = "15") String size,
      @RequestParam(defaultValue = "true") boolean saveRecent) {
    BookSearchResDTO response = bookQueryService.searchExternalBooks(query, page, size, saveRecent);
    return ResponseEntity.ok(
        ApiResponse.onSuccess(BookSuccessCode.EXTERNAL_BOOK_SEARCHED, response));
  }

  @Override
  @GetMapping("/api/v1/books/{isbn}")
  public ResponseEntity<ApiResponse<BookDetailResDTO>> getBookDetail(@PathVariable String isbn) {
    BookDetailResDTO response = bookQueryService.getBookDetail(isbn);
    return ResponseEntity.ok(ApiResponse.onSuccess(BookSuccessCode.BOOK_DETAIL_FOUND, response));
  }

  @Override
  @GetMapping("/api/v1/books/search/recent")
  public ResponseEntity<ApiResponse<RecentBookSearchResDTO>> getRecentBookSearches() {
    RecentBookSearchResDTO response = bookQueryService.getRecentBookSearches();
    return ResponseEntity.ok(
        ApiResponse.onSuccess(BookSuccessCode.RECENT_BOOK_SEARCHES_FOUND, response));
  }

  @Override
  @GetMapping("/api/v1/member-books")
  public ResponseEntity<ApiResponse<MemberBookListResDTO>> getMemberBooks(
      @RequestParam(defaultValue = "ALL") String status,
      @RequestParam(defaultValue = "1") String page,
      @RequestParam(defaultValue = "20") String size) {
    MemberBookListResDTO response = bookQueryService.getMemberBooks(status, page, size);
    return ResponseEntity.ok(
        ApiResponse.onSuccess(BookSuccessCode.MEMBER_BOOK_LIST_FOUND, response));
  }

  @Override
  @PutMapping("/api/v1/member-books/{isbn}")
  public ResponseEntity<ApiResponse<MemberBookUpsertResDTO>> upsertMemberBook(
      @PathVariable String isbn, @Valid @RequestBody MemberBookUpsertReqDTO request) {
    BookCommandService.MemberBookUpsertResult result =
        bookCommandService.upsertMemberBook(isbn, request);
    BookSuccessCode successCode =
        result.created()
            ? BookSuccessCode.MEMBER_BOOK_CREATED
            : BookSuccessCode.MEMBER_BOOK_UPDATED;

    return ResponseEntity.status(successCode.getStatus())
        .body(ApiResponse.onSuccess(successCode, result.response()));
  }
}
