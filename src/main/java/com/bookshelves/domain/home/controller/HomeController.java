package com.bookshelves.domain.home.controller;

import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.domain.home.exception.code.HomeSuccessCode;
import com.bookshelves.domain.home.service.HomeQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController implements HomeControllerDocs {

  private final HomeQueryService homeQueryService;

  @Override
  @GetMapping("/api/v1/home")
  public ResponseEntity<ApiResponse<HomeResDTO>> getHome() {
    HomeResDTO response = homeQueryService.getHome();
    return ResponseEntity.ok(ApiResponse.onSuccess(HomeSuccessCode.HOME_FOUND, response));
  }
}
