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
          홈 화면에 필요한 회원 정보, 오늘의 추천 도서, 최근 본 책과 모집 중인 모임을 한 번에 조회합니다.

          오늘의 추천 도서는 전날 23시 스케줄러가 하루 한 권을 미리 준비합니다. 회원별 개인화가 아니라
          모든 사용자가 같은 책을 봅니다. 후보는 문학(KDC 800)과 철학(KDC 100)으로 분류된 책으로 한정하며,
          KDC 정보가 없는 책은 추천 후보에서 제외합니다. 해당 날짜의 추천이 준비되지 않았다면 가장 최근
          추천을 반환하며, 실제 노출 기준일은 `recommendedAt`에서 확인할 수 있습니다. 저장된 추천이 하나도
          없으면 `recommendedAt`과 `recommendedBook`이 모두 `null`입니다.

          최근 본 책은 서재 기록을 마지막으로 수정한 책입니다. 기록이 하나도 없으면 null입니다.

          모집 중인 모임은 현재 참여할 수 있는 항목만 반환합니다. 정원이 찼거나 모집 마감 시각이 지났거나
          이미 참여 중인 모임은 제외하며, 시작 시각이 빠른 순서로 최대 3건입니다.

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
                    "book": {
                      "id": 1,
                      "isbn": "9788936434267",
                      "title": "아몬드",
                      "author": "손원평",
                      "publisher": "창비",
                      "coverImageUrl": "https://example.com/almond.jpg",
                      "kdcCode": "813",
                      "kdcName": "한국소설"
                    }
                  },
                  "recentBook": {
                    "memberBook": {
                      "id": 10,
                      "progress": 70,
                      "status": "READING",
                      "rating": 4.5,
                      "updatedAt": "2026-07-30T04:30:00"
                    },
                    "book": {
                      "id": 2,
                      "isbn": "9788936434595",
                      "title": "혼모노",
                      "author": "성해나",
                      "publisher": "창비",
                      "coverImageUrl": "https://example.com/honmono.jpg",
                      "kdcCode": "813",
                      "kdcName": "한국소설"
                    }
                  },
                  "meetings": [
                    {
                      "meeting": {
                        "id": 21,
                        "status": "RECRUITING",
                        "startDate": "2026-08-02T19:00:00",
                        "currentParticipants": 4,
                        "maxParticipants": 6,
                        "duration": 30
                      },
                      "book": {
                        "id": 2,
                        "title": "혼모노",
                        "author": "성해나",
                        "publisher": "창비",
                        "coverImageUrl": "https://example.com/honmono.jpg"
                      }
                    }
                  ]
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "유효하지 않거나 만료된 Access Token (AUTH401_2)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "인증 실패",
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "AUTH401_2",
                              "message": "유효하지 않은 Access Token입니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "회원을 찾을 수 없음 (MEMBER404_1)",
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
                "result": {}
              }
              """)))
  })
  ResponseEntity<ApiResponse<HomeResDTO>> getHome();
}
