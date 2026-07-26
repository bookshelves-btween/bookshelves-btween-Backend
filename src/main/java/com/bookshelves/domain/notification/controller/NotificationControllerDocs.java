package com.bookshelves.domain.notification.controller;

import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @Operation(summary = "알림 목록 조회", description = "인증된 사용자의 알림을 생성 시각과 ID 기준 최신순으로 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "알림 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "NOTI200_2",
                "message": "알림 목록 조회에 성공했습니다.",
                "result": {
                  "notifications": [
                    {
                      "id": 102,
                      "type": "SYSTEM",
                      "title": "서비스 점검 안내",
                      "content": "7월 20일 02:00 ~ 03:00 시스템 점검이 진행됩니다.",
                      "isRead": false,
                      "targetId": null,
                      "createdAt": "2026-07-14T21:00:00"
                    },
                    {
                      "id": 101,
                      "type": "MEETING_STARTED",
                      "title": "모임이 곧 시작됩니다.",
                      "content": "10분 후 모임이 시작됩니다.",
                      "isRead": false,
                      "targetId": 12,
                      "createdAt": "2026-07-14T20:00:00"
                    }
                  ],
                  "page": 1,
                  "size": 20,
                  "hasNext": false
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "page 또는 size 요청값 검증 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {}
              }
              """))),
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
              """)))
  })
  @GetMapping("/api/v1/notifications")
  ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @Parameter(description = "조회할 페이지 번호", example = "1")
          @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
          Integer page,
      @Parameter(description = "한 페이지당 조회할 알림 개수", example = "20")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
          Integer size);

  @Operation(summary = "알림 읽음 처리", description = "인증된 사용자의 알림을 읽음 처리합니다. 이미 읽은 알림도 동일하게 성공 응답합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "알림 읽음 처리 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "NOTI200_3",
                "message": "알림을 읽음 처리했습니다.",
                "result": {
                  "id": 101
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "notificationId가 1보다 작음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {}
              }
              """))),
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
        responseCode = "404",
        description = "알림이 존재하지 않거나 현재 사용자의 알림이 아님",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "NOTI404_1",
                "message": "존재하지 않는 알림입니다.",
                "result": {}
              }
              """)))
  })
  @PatchMapping("/api/v1/notifications/{notificationId}/read")
  ResponseEntity<ApiResponse<NotificationReadResponse>> readNotification(
      @Parameter(description = "읽음 처리할 알림 ID", example = "101")
          @Min(value = 1, message = "알림 ID는 1 이상이어야 합니다.")
          @PathVariable(name = "notificationId")
          Long notificationId);
}
