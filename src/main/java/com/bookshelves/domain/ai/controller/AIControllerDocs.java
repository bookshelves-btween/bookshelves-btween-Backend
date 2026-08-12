package com.bookshelves.domain.ai.controller;

import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "AI", description = "독서 모임 질문 공개 투표 API")
public interface AIControllerDocs {

  @Operation(
      summary = "다음 질문 공개 투표",
      description =
          """
          진행 중인 독서 모임에서 다음 질문을 공개하기 위한 투표를 반영합니다.

          정족수는 현재 채팅방 접속 인원의 절반을 올림한 값입니다. 예를 들어 접속자가 5명이면 3표가
          필요합니다. 한 회원은 질문 라운드마다 한 번만 투표할 수 있습니다.

          투표가 반영되면 채팅방 구독 경로(`/sub/chatrooms/{chatroomId}`)로 `VOTE_COUNT` 프레임을
          전송합니다. 정족수에 도달하면 `triggered`가 `true`가 되고, 미리 준비된 다음 질문을 `QUESTION`
          프레임으로 전송한 뒤 투표 수를 초기화합니다. 다음 질문은 이 HTTP 응답에 포함되지 않습니다.
          """)
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "투표 반영 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "정족수 도달",
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "AI200_1",
                              "message": "질문 생성 투표가 반영되었습니다.",
                              "result": {
                                "currentVotes": 3,
                                "requiredVotes": 3,
                                "triggered": true
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
        description = "모임 참여자가 아님 (AI403_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "AI403_1",
                              "message": "모임 참여자만 투표할 수 있습니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "모임 또는 채팅방을 찾을 수 없음 (MEETING404_1, CHAT404_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "모임 없음",
                      value =
                          """
                            {
                              "isSuccess": false,
                              "code": "MEETING404_1",
                              "message": "해당 모임을 찾을 수 없습니다.",
                              "result": {}
                            }
                            """),
                  @ExampleObject(
                      name = "채팅방 없음",
                      value =
                          """
                            {
                              "isSuccess": false,
                              "code": "CHAT404_1",
                              "message": "존재하지 않는 채팅방입니다.",
                              "result": {}
                            }
                            """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "중복 투표, 마지막 질문 공개 완료 또는 진행 중이 아닌 모임",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "중복 투표",
                      value =
                          """
                          {
                            "isSuccess": false,
                            "code": "AI409_1",
                            "message": "이미 이번 질문에 투표했습니다.",
                            "result": {}
                          }
                          """),
                  @ExampleObject(
                      name = "마지막 질문까지 공개됨",
                      value =
                          """
                          {
                            "isSuccess": false,
                            "code": "AI409_2",
                            "message": "질문을 더 생성할 수 없습니다.",
                            "result": {}
                          }
                          """),
                  @ExampleObject(
                      name = "진행 중이 아닌 모임",
                      value =
                          """
                          {
                            "isSuccess": false,
                            "code": "AI409_3",
                            "message": "진행 중인 모임이 아닙니다.",
                            "result": {}
                          }
                          """)
                }))
  })
  @PostMapping("/api/v1/meetings/{meetingId}/question-votes")
  ResponseEntity<ApiResponse<QuestionVoteResponse>> voteForNewQuestion(
      @Parameter(description = "투표할 모임 ID", example = "21", required = true) @PathVariable
          Long meetingId);
}
