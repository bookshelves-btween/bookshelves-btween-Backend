package com.bookshelves.domain.notification.controller;

import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "알림", description = "알림 API")
public interface NotificationControllerDocs {

  @Operation(
      summary = "FCM 디바이스 토큰 등록",
      description =
          "인증된 사용자의 FCM 토큰을 등록합니다. 동일한 토큰이 이미 등록되어 있으면 현재 사용자에게 다시 연결하며, "
              + "MVP에서는 platform을 요청으로 받지 않고 IOS로 저장합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "FCM 토큰 등록 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "NOTI200_1",
                "message": "FCM 토큰이 등록되었습니다.",
                "result": null
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 검증 실패 또는 잘못된 JSON 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "FCM 토큰 누락 또는 빈 값",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {
                  "fcmToken": "공백일 수 없습니다"
                }
              }
              """),
                  @ExampleObject(
                      name = "FCM 토큰 길이 초과",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {
                  "fcmToken": "크기가 0에서 255 사이여야 합니다"
                }
              }
              """),
                  @ExampleObject(
                      name = "잘못된 JSON",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {}
              }
              """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "access token이 없거나 만료·서명 불일치 등으로 유효하지 않음",
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
        responseCode = "415",
        description = "지원하지 않는 Content-Type",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "COMMON415_1",
                "message": "지원하지 않는 Content-Type입니다.",
                "result": {}
              }
              """)))
  })
  @PostMapping("/api/v1/notifications/fcm/tokens")
  ResponseEntity<ApiResponse<Void>> registerFcmToken(
      @Valid @RequestBody FcmTokenRegisterRequest request);
}
