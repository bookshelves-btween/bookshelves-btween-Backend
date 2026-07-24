package com.bookshelves.domain.member.controller;

import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.dto.response.MemberWithdrawResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
                  "nickname": "책 먹는 여우",
                  "nicknameNoun": "책",
                  "nicknameModifier": "먹는",
                  "nicknameAnimal": "여우",
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

  @Operation(
      summary = "내 정보 수정",
      description =
          "닉네임(noun/modifier/animal 3조각 전부 함께 전달), 프로필 배경색, 관심 카테고리를 수정한다. "
              + "각 필드는 선택적이며, 보낸 필드만 반영된다. categoryIds는 완전 교체 방식이며 "
              + "필드 자체를 보내지 않으면 기존 선택이 유지된다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "수정 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEMBER200_2",
                "message": "회원 정보 수정에 성공하였습니다.",
                "result": {
                  "id": 1,
                  "nickname": "책 먹는 여우",
                  "nicknameNoun": "책",
                  "nicknameModifier": "먹는",
                  "nicknameAnimal": "여우",
                  "profileBackgroundColor": "ORANGE",
                  "provider": "KAKAO",
                  "memberStatus": "ACTIVE",
                  "createdAt": "2026-07-01T10:00:00",
                  "categories": [
                    { "id": 3, "name": "자기계발" }
                  ]
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "수정할 필드가 없거나, 닉네임 3조각이 일부만 오거나, 존재하지 않는 카테고리 ID를 포함함",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "수정할 필드 없음",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "MEMBER400_2",
                "message": "수정할 회원 정보가 없습니다.",
                "result": {}
              }
              """),
                  @ExampleObject(
                      name = "잘못된 요청(닉네임 일부만 전달/존재하지 않는 카테고리 등)",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "MEMBER400_1",
                "message": "유효하지 않은 요청입니다.",
                "result": {}
              }
              """)
                })),
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
  @PatchMapping("/api/v1/members/me")
  ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(
      @Valid @RequestBody MemberUpdateRequest request);

  @Operation(
      summary = "회원 탈퇴",
      description =
          "현재 로그인된 회원을 탈퇴 처리한다. 탈퇴 시점으로부터 30일 이내에 동일 계정으로 재로그인하면 계정을 복구할 수 있으며, "
              + "그 안내를 위해 복구 가능 기한을 응답으로 반환한다. 탈퇴 즉시 refresh token이 무효화되어 재로그인 전까지는 어떤 API도 호출할 수 없다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "탈퇴 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEMBER200_3",
                "message": "회원 탈퇴가 요청되었습니다.",
                "result": {
                  "scheduledDeletionAt": "2026-08-21T14:30:00+09:00"
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
  @DeleteMapping("/api/v1/members/me")
  ResponseEntity<ApiResponse<MemberWithdrawResponse>> withdraw();
}
