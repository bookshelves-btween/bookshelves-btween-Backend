package com.bookshelves.domain.report.controller;

import com.bookshelves.domain.report.dto.ReportCreateRequest;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "신고", description = "채팅방 신고 API")
public interface ReportControllerDocs {

  @Operation(
      summary = "채팅방 신고",
      description =
          """
          채팅방에서 발생한 문제를 신고합니다. 신고자는 Access Token으로 식별하므로 요청 본문에는
          채팅방 ID만 전달합니다.

          해당 모임의 참여자만 신고할 수 있으며, 신고할 대화가 존재하는 모임 시작 이후부터 가능합니다.
          한 회원은 같은 채팅방을 한 번만 신고할 수 있습니다.
          """)
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "신고 접수 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "REPORT201_1",
                              "message": "신고가 접수되었습니다.",
                              "result": {
                                "id": 15,
                                "chatroomId": 7,
                                "status": "PENDING",
                                "createdAt": "2026-08-12T19:20:31"
                              }
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "chatroomId가 누락된 요청 (COMMON400_1)",
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
                              "result": { "chatroomId": "널이어서는 안됩니다" }
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
        responseCode = "403",
        description = "모임 참여자가 아님 (REPORT403_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "REPORT403_1",
                              "message": "모임 참여자만 신고할 수 있습니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음 (REPORT404_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "REPORT404_1",
                              "message": "존재하지 않는 채팅방입니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복 신고 또는 시작 전 모임",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "중복 신고",
                      value =
                          """
                          {
                            "isSuccess": false,
                            "code": "REPORT409_1",
                            "message": "이미 신고한 채팅방입니다.",
                            "result": {}
                          }
                          """),
                  @ExampleObject(
                      name = "모임 시작 전",
                      value =
                          """
                          {
                            "isSuccess": false,
                            "code": "REPORT409_2",
                            "message": "모임이 시작된 뒤에 신고할 수 있습니다.",
                            "result": {}
                          }
                          """)
                }))
  })
  ResponseEntity<ApiResponse<ReportCreateResponse>> createReport(
      @Valid @RequestBody ReportCreateRequest request);
}
