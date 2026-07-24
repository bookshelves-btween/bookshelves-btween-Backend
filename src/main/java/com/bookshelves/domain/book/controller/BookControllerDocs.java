package com.bookshelves.domain.book.controller;

import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "도서", description = "도서·내 서재 API")
public interface BookControllerDocs {

  @Operation(summary = "카테고리 목록 조회", description = "KDC 최상위 분류 10개를 코드 오름차순으로 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "카테고리 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_1",
                              "message": "카테고리 목록 조회에 성공했습니다.",
                              "result": {
                                "categories": [
                                  { "id": 1, "kdcCode": "000", "name": "총류" },
                                  { "id": 2, "kdcCode": "100", "name": "철학" },
                                  { "id": 3, "kdcCode": "200", "name": "종교" },
                                  { "id": 4, "kdcCode": "300", "name": "사회과학" },
                                  { "id": 5, "kdcCode": "400", "name": "자연과학" },
                                  { "id": 6, "kdcCode": "500", "name": "기술과학" },
                                  { "id": 7, "kdcCode": "600", "name": "예술" },
                                  { "id": 8, "kdcCode": "700", "name": "언어" },
                                  { "id": 9, "kdcCode": "800", "name": "문학" },
                                  { "id": 10, "kdcCode": "900", "name": "역사" }
                                ]
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "유효하지 않은 Access Token",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "AUTH401_2",
                              "message": "유효하지 않은 Access Token입니다.",
                              "result": null
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "카테고리 목록 조회 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK500_1",
                              "message": "카테고리 목록 조회에 실패했습니다.",
                              "result": {}
                            }
                            """)))
  })
  ResponseEntity<ApiResponse<CategoryListResDTO>> getCategories();
}
