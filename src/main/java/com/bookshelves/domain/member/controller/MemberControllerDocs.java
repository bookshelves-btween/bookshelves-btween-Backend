package com.bookshelves.domain.member.controller;

import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "회원", description = "회원·온보딩 API")
public interface MemberControllerDocs {

  @Operation(summary = "내 정보 조회", description = "현재 로그인된 회원의 프로필 정보와 선택한 관심 카테고리를 조회한다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEMBER200_1",
                "message": "내 정보 조회에 성공하였습니다.",
                "result": {
                  "id": 1,
                  "nickname": "행복한 사자",
                  "nicknameNoun": "사자",
                  "nicknameModifier": "행복한",
                  "nicknameAnimal": "사자",
                  "profileBackgroundColor": "ORANGE",
                  "provider": "KAKAO",
                  "memberStatus": "ACTIVE",
                  "createdAt": "2026-07-01T10:00:00",
                  "categories": [
                    { "id": 1, "name": "소설" },
                    { "id": 2, "name": "에세이" }
                  ]
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패 — access token이 없거나, 만료·서명 불일치 등으로 유효하지 않음",
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
        responseCode = "404",
        description = "회원을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
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
  @GetMapping("/api/v1/members/me")
  ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo();
}
