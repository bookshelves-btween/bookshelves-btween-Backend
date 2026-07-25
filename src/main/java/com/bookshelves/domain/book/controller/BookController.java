package com.bookshelves.domain.book.controller;

import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.exception.code.BookSuccessCode;
import com.bookshelves.domain.book.service.BookQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookController implements BookControllerDocs {

  private final BookQueryService bookQueryService;

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
      @RequestParam(defaultValue = "15") String size) {
    BookSearchResDTO response = bookQueryService.searchExternalBooks(query, page, size);
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
}
