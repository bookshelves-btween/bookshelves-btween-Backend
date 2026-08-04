package com.bookshelves.domain.book.controller;

import com.bookshelves.domain.book.dto.request.MemberBookUpsertReqDTO;
import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookCalendarResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookStatisticsResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookUpsertResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "도서", description = "도서·내 서재 API")
public interface BookControllerDocs {

  @Operation(summary = "독서 통계 조회", description = "로그인한 회원의 완독 도서 수, 한줄평 작성 수, 평균 별점을 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "독서 통계 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_8",
                              "message": "독서 통계 조회에 성공했습니다.",
                              "result": {
                                "year": 2026,
                                "month": 6,
                                "completedBookCount": 24,
                                "reviewCount": 17,
                                "averageRating": 4.0,
                                "categoryStatistics": [
                                  { "name": "한국 문학", "count": 14, "percentage": 58 },
                                  { "name": "영미문학", "count": 6, "percentage": 25 },
                                  { "name": "심리학", "count": 4, "percentage": 17 }
                                ]
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "year 또는 month 값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "독서 통계 조회 실패 (BOOK500_5)")
  })
  ResponseEntity<ApiResponse<MemberBookStatisticsResDTO>> getMemberBookStatistics(
      @Parameter(description = "조회 연도. 없으면 현재 연도", example = "2026") String year,
      @Parameter(description = "조회 월. 없으면 현재 월", example = "6") String month);

  @Operation(summary = "독서 캘린더 조회", description = "지정한 연·월의 실제 독서 진행 기록을 날짜별로 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "독서 캘린더 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_7",
                              "message": "독서 캘린더 조회에 성공했습니다.",
                              "result": {
                                "year": 2026,
                                "month": 7,
                                "days": [{
                                  "date": "2026-07-14",
                                  "coverImageUrl": "https://image.example.com/almond.jpg"
                                }]
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "year 또는 month 값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "독서 캘린더 조회 실패 (BOOK500_4)")
  })
  ResponseEntity<ApiResponse<MemberBookCalendarResDTO>> getMemberBookCalendar(
      @Parameter(description = "조회 연도", example = "2026", required = true) String year,
      @Parameter(description = "조회 월", example = "7", required = true) String month);

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

  @Operation(summary = "내 서재 목록 조회", description = "로그인한 회원의 독서 기록을 최근 수정 순으로 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "내 서재 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_6",
                              "message": "내 서재 목록 조회에 성공했습니다.",
                              "result": {
                                "memberBooks": [{
                                  "memberBook": {
                                    "id": 10,
                                    "progress": 70,
                                    "status": "READING",
                                    "rating": 4.5,
                                    "memo": "기억에 남는 책입니다.",
                                    "updatedAt": "2026-07-14T04:30:00"
                                  },
                                  "book": {
                                    "id": 1,
                                    "isbn": "9788936434595",
                                    "title": "아몬드",
                                    "author": "손원평",
                                    "publisher": "창비",
                                    "coverImageUrl": "https://image.example.com/book.jpg",
                                    "kdcCode": "813",
                                    "kdcName": "문학"
                                  }
                                }],
                                "page": 1,
                                "size": 20,
                                "hasNext": false
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "독서 상태, page 또는 size 값 오류"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요")
  })
  ResponseEntity<ApiResponse<MemberBookListResDTO>> getMemberBooks(
      @Parameter(description = "ALL, BEFORE_READING, READING, or FINISHED", example = "ALL")
          String status,
      @Parameter(description = "Page number, starting at 1", example = "1") String page,
      @Parameter(description = "Page size, from 1 to 50", example = "20") String size);

  @Operation(
      summary = "외부 도서 검색",
      description =
          "카카오 도서 검색 API를 정확도순으로 한 번 호출합니다. ISBN13을 우선해 정규화하고 saveRecent가 true일 때 회원의 최근 검색어를 저장합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "도서 검색 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_2",
                              "message": "도서 검색에 성공했습니다.",
                              "result": {
                                "books": [{
                                  "isbn": "9788936434595",
                                  "title": "혼모노",
                                  "author": "성해나",
                                  "publisher": "창비",
                                  "publishedDate": "2024-03-29",
                                  "description": "도서 설명입니다.",
                                  "coverImageUrl": "https://image.example.com/book.jpg",
                                  "saveable": true
                                }],
                                "page": 1,
                                "size": 15,
                                "hasNext": false
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "잘못된 검색 조건",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK400_1",
                              "message": "검색어, page 또는 size 값이 올바르지 않습니다.",
                              "result": {}
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
        responseCode = "502",
        description = "카카오 도서 API 호출 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK502_1",
                              "message": "외부 도서 API 호출에 실패했습니다.",
                              "result": {}
                            }
                            """)))
  })
  ResponseEntity<ApiResponse<BookSearchResDTO>> searchExternalBooks(
      @Parameter(description = "책 제목 또는 저자", example = "혼모노", required = true) String query,
      @Parameter(
              description = "페이지 번호(1~50)",
              example = "1",
              schema = @Schema(type = "integer", minimum = "1", maximum = "50"))
          String page,
      @Parameter(
              description = "페이지 크기(1~50)",
              example = "15",
              schema = @Schema(type = "integer", minimum = "1", maximum = "50"))
          String size,
      @Parameter(
              description = "최근 도서 검색어 저장 여부입니다. 기본값은 true이며, 모임 검색에서는 false를 전달합니다.",
              example = "true",
              schema = @Schema(type = "boolean", defaultValue = "true"))
          boolean saveRecent);

  @Operation(summary = "최근 검색어 조회", description = "회원의 최근 도서 검색어를 검색 시각 내림차순으로 최대 5개 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "최근 검색어 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_3",
                              "message": "최근 검색어 조회에 성공했습니다.",
                              "result": {
                                "recentSearches": [{
                                  "keyword": "혼모노",
                                  "searchedAt": "2026-07-13T10:30:00+09:00"
                                }]
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
        description = "최근 검색어 조회 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK500_2",
                              "message": "최근 검색어를 불러오지 못했습니다.",
                              "result": {}
                            }
                            """)))
  })
  ResponseEntity<ApiResponse<RecentBookSearchResDTO>> getRecentBookSearches();

  @Operation(
      summary = "최근 검색어 삭제",
      description = "로그인한 회원의 최근 검색어 중 keyword와 일치하는 항목을 삭제합니다. 항목이 없어도 성공합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "최근 검색어 삭제 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_10",
                              "message": "최근 검색어 삭제에 성공했습니다.",
                              "result": null
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "삭제할 검색어 누락 또는 공백 (BOOK400_7)"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "최근 검색어 삭제 실패 (BOOK500_6)")
  })
  ResponseEntity<ApiResponse<Void>> deleteRecentBookSearch(
      @Parameter(description = "삭제할 최근 검색어", example = "혼모노", required = true) String keyword);

  @Operation(summary = "책 상세 조회", description = "ISBN으로 외부 도서 정보와 내 독서 기록을 함께 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "책 상세 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "저장된 책",
                      value =
                          """
                          {
                            "isSuccess": true,
                            "code": "BOOK200_4",
                            "message": "책 상세 조회에 성공했습니다.",
                            "result": {
                              "book": {
                                "id": 10,
                                "isbn": "9788936434595",
                                "title": "혼모노",
                                "author": "성해나",
                                "publisher": "창비",
                                "publishedDate": "2024-03-29",
                                "description": "도서 설명입니다.",
                                "coverImageUrl": "https://image.example.com/book.jpg",
                                "kdcCode": "813",
                                "kdcName": "문학"
                              },
                              "memberBook": {
                                "id": 20,
                                "progress": 70,
                                "rating": 4.5,
                                "memo": "진짜란 무엇인가?"
                              }
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "저장 전 책",
                      value =
                          """
                          {
                            "isSuccess": true,
                            "code": "BOOK200_4",
                            "message": "책 상세 조회에 성공했습니다.",
                            "result": {
                              "book": {
                                "id": null,
                                "isbn": "9788936434595",
                                "title": "혼모노",
                                "author": "성해나",
                                "publisher": "창비",
                                "publishedDate": "2024-03-29",
                                "description": "도서 설명입니다.",
                                "coverImageUrl": "https://image.example.com/book.jpg",
                                "kdcCode": "813",
                                "kdcName": "문학"
                              },
                              "memberBook": null
                            }
                          }
                          """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "ISBN 형식 오류",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK400_2",
                              "message": "ISBN 형식이 올바르지 않습니다.",
                              "result": null
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "책을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK404_1",
                              "message": "해당 책을 찾을 수 없습니다.",
                              "result": null
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "외부 도서 API 호출 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "BOOK502_1",
                              "message": "외부 도서 API 호출에 실패했습니다.",
                              "result": null
                            }
                            """)))
  })
  ResponseEntity<ApiResponse<BookDetailResDTO>> getBookDetail(
      @Parameter(description = "ISBN10 또는 ISBN13", example = "9788936434595", required = true)
          String isbn);

  @Operation(
      summary = "내 서재 독서 기록 저장·수정",
      description = "ISBN으로 책을 내 서재에 저장하거나 기존 독서 기록을 전체 수정합니다. 진행률이 증가한 경우에만 독서 이력이 생성됩니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "독서 기록 저장 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK201_1",
                              "message": "독서 기록 저장에 성공했습니다.",
                              "result": { "memberBookHistory": { "id": 1 } }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "독서 기록 수정 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_5",
                              "message": "독서 기록 수정에 성공했습니다.",
                              "result": { "memberBookHistory": null }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "유효하지 않은 ISBN 또는 독서 기록 요청",
        content = @Content(mediaType = "application/json")),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(mediaType = "application/json")),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "책을 찾을 수 없음",
        content = @Content(mediaType = "application/json")),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "502",
        description = "외부 도서 API 호출 실패",
        content = @Content(mediaType = "application/json"))
  })
  ResponseEntity<ApiResponse<MemberBookUpsertResDTO>> upsertMemberBook(
      @Parameter(description = "ISBN10 또는 ISBN13", example = "9788936434595", required = true)
          String isbn,
      @Valid MemberBookUpsertReqDTO request);

  @Operation(
      summary = "내 서재 독서 기록 삭제",
      description = "로그인한 회원의 ISBN 기준 독서 기록과 진행률 이력을 삭제합니다. 책 자체는 삭제하지 않습니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "독서 기록 삭제 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "BOOK200_9",
                              "message": "독서 기록 삭제에 성공했습니다.",
                              "result": null
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "유효하지 않은 ISBN"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 필요"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "내 서재 독서 기록을 찾을 수 없음")
  })
  ResponseEntity<ApiResponse<Void>> deleteMemberBook(
      @Parameter(description = "ISBN10 또는 ISBN13", example = "9788936434595", required = true)
          String isbn);
}
