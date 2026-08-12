package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "채팅", description = "모임 채팅 API")
public interface ChatControllerDocs {

  @Operation(
      summary = "채팅방 입장 정보 조회",
      description =
          """
          채팅방 화면을 구성하는 모임 정보, 접속 인원, 현재 질문, 투표 현황과 전체 메시지를 조회합니다.
          메시지는 오래된 순서부터 반환합니다.

          모임 시작 전에는 `currentQuestion`이 `null`입니다. 재연결할 때도 이 API를 다시 호출한 뒤
          `messageId`를 기준으로 기존 메시지와 중복되지 않게 합칠 수 있습니다.

          입장 이후 실시간 변경 사항은 `/sub/chatrooms/{chatroomId}`를 구독해 받습니다. 이 구독에는
          `MESSAGE`, `PARTICIPANT`, `QUESTION`, `VOTE_COUNT`, `SYSTEM` 프레임이 전달됩니다.
          """)
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "채팅방 입장 정보 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": true,
                              "code": "CHAT200_1",
                              "message": "채팅방 입장에 성공했습니다.",
                              "result": {
                                "chatroomId": 7,
                                "meetingId": 21,
                                "bookTitle": "아몬드",
                                "status": "IN_PROGRESS",
                                "startsAt": "2026-08-12T19:00:00+09:00",
                                "endsAt": "2026-08-12T20:00:00+09:00",
                                "maxParticipants": 6,
                                "participants": { "applied": 5, "connected": 4 },
                                "myMemberId": 12,
                                "currentQuestion": {
                                  "questionId": 101,
                                  "questionOrder": 2,
                                  "content": "이 책에 별점을 준다면 몇 점이며, 그 이유는 무엇인가요?"
                                },
                                "maxQuestions": 5,
                                "vote": { "currentVotes": 1, "requiredVotes": 2, "voted": true },
                                "messages": [
                                  {
                                    "messageId": 301,
                                    "senderMemberId": 12,
                                    "senderNickname": "책 먹는 여우",
                                    "senderNicknameAnimal": "여우",
                                    "senderProfileBackgroundColor": "YELLOW",
                                    "content": "저는 주인공의 변화가 가장 인상 깊었어요.",
                                    "createdAt": "2026-08-12T19:05:12+09:00"
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
        description = "채팅방이 속한 모임의 참여자가 아님 (CHAT403_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "CHAT403_1",
                              "message": "채팅방에 참여하지 않은 회원입니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "채팅방을 찾을 수 없음 (CHAT404_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "CHAT404_1",
                              "message": "존재하지 않는 채팅방입니다.",
                              "result": {}
                            }
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "410",
        description = "이미 종료된 모임의 채팅방 (CHAT410_1)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "isSuccess": false,
                              "code": "CHAT410_1",
                              "message": "이미 종료된 모임입니다.",
                              "result": {}
                            }
                            """)))
  })
  @GetMapping("/api/v1/chatrooms/{chatroomId}")
  ResponseEntity<ApiResponse<ChatRoomEnterResponse>> enterChatRoom(
      @Parameter(description = "입장할 채팅방 ID", example = "7", required = true) @PathVariable
          Long chatroomId);
}
