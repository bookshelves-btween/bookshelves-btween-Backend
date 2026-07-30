package com.bookshelves.domain.home.controller;

import com.bookshelves.domain.home.dto.response.HomeResDTO;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "홈", description = "홈 화면 API")
public interface HomeControllerDocs {

  @Operation(
      summary = "홈 화면 조회",
      description =
          """
          홈 화면에 필요한 오늘의 추천 도서, 최근 본 책, 모집중 모임을 한 번에 조회합니다.

          오늘의 추천 도서는 전날 23시 스케줄러가 하루 한 권을 미리 준비합니다. 회원별 개인화가 아니라
          모든 사용자가 같은 책을 봅니다. 스케줄러가 건너뛴 날은 가장 최근에 준비된 추천으로 내려가며,
          어느 날짜의 추천인지는 recommendedAt으로 구분합니다. 책이 아직 한 권도 등록되지 않은 경우에만
          recommendedBook이 null입니다.

          최근 본 책은 서재 기록을 마지막으로 수정한 책입니다. 기록이 하나도 없으면 null입니다.

          모집중 모임은 지금 참여하기를 누를 수 있는 모임만 담습니다. 정원이 찼거나 이미 시작했거나
          내가 이미 참여 중인 모임은 제외하며, 시작이 빠른 순으로 최대 3건입니다.

          요일은 startDate에서 클라이언트가 계산합니다.
          """)
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "홈 화면 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "홈 화면 조회 성공",
                        value =
                            """
              {
                "isSuccess": true,
                "code": "HOME200_1",
                "message": "홈 화면 조회에 성공했습니다.",
                "result": {
                  "member": { "nickname": "책 먹는 여우" },
                  "recommendedAt": "2026-07-31",
                  "recommendedBook": {
                    "recommendationMessage": "감정을 배우는 소년의 조용한 성장 기록",
                    "bookId": 1,
                    "title": "아몬드",
                    "author": "손원평",
                    "publisher": "창비",
                    "kdcName": "한국소설",
                    "coverImageUrl": "https://example.com/almond.jpg"
                  },
                  "recentBook": {
                    "bookId": 2,
                    "title": "혼모노",
                    "author": "성해나",
                    "publisher": "창비",
                    "coverImageUrl": "https://example.com/honmono.jpg",
                    "rating": 4.5,
                    "progress": 70
                  },
                  "meetings": [
                    {
                      "meetingId": 21,
                      "title": "혼모노",
                      "coverImageUrl": "https://example.com/honmono.jpg",
                      "startDate": "2026-08-02T19:00:00",
                      "currentParticipants": 4,
                      "maxParticipants": 6
                    }
                  ]
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "회원 없음",
                        value =
                            """
              {
                "isSuccess": false,
                "code": "MEMBER404_1",
                "message": "회원을 찾을 수 없습니다.",
                "result": null
              }
              """)))
  })
  ResponseEntity<ApiResponse<HomeResDTO>> getHome();
}
