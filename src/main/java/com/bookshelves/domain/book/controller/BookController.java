package com.bookshelves.domain.book.controller;

import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.exception.code.BookSuccessCode;
import com.bookshelves.domain.book.service.BookQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
