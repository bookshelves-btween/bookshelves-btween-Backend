package com.bookshelves.domain.meeting.controller;

import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "모임", description = "독서모임 API")
public interface MeetingControllerDocs {

  @Operation(summary = "모임 상세 조회", description = "모임과 도서 정보 및 질문별 모임 요약을 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "모임 상세 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "요약 완료",
                      summary = "완료된 모임에 요약이 존재하는 경우",
                      value =
                          """
                          {
                            "isSuccess": true,
                            "code": "MEETING200_1",
                            "message": "모임 상세 조회에 성공했습니다.",
                            "result": {
                              "id": 1004,
                              "chatroomId": 1004,
                              "status": "COMPLETED",
                              "startDate": "2026-06-10T20:00:00",
                              "duration": 90,
                              "currentParticipants": 4,
                              "maxParticipants": 4,
                              "book": {
                                "id": 1001,
                                "title": "소년이 온다",
                                "description": "한 도시의 비극과 그 이후를 여러 인물의 시선으로 그린 장편소설.",
                                "author": "한강",
                                "publisher": "창비",
                                "coverImageUrl": "https://contents.kyobobook.co.kr/sih/fit-in/458x0/pdt/9788936434267.jpg",
                                "kdcName": "한국소설"
                              },
                              "meetingSummary": [
                                {
                                  "questionOrder": 1,
                                  "question": "각 화자의 시선이 사건을 이해하는 데 어떤 차이를 만들었나요?",
                                  "summary": "참여자들은 서로 다른 화자의 기억이 하나의 사건을 입체적으로 구성한다는 데 의견을 모았다."
                                },
                                {
                                  "questionOrder": 2,
                                  "question": "이 작품이 오늘날 우리에게 던지는 질문은 무엇인가요?",
                                  "summary": "과거의 고통을 기억하고 타인의 아픔에 응답하는 책임에 관해 토론했다."
                                }
                              ]
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "요약 미완료",
                      summary = "모임 요약을 제공하지 않는 경우",
                      value =
                          """
                          {
                            "isSuccess": true,
                            "code": "MEETING200_1",
                            "message": "모임 상세 조회에 성공했습니다.",
                            "result": {
                              "id": 1001,
                              "chatroomId": 1001,
                              "status": "RECRUITING",
                              "startDate": "2026-08-01T20:00:00",
                              "duration": 60,
                              "currentParticipants": 3,
                              "maxParticipants": 4,
                              "book": {
                                "id": 1002,
                                "title": "우리가 빛의 속도로 갈 수 없다면",
                                "description": "과학기술이 발전한 세계에서도 남겨지는 사람들을 다룬 SF 소설집.",
                                "author": "김초엽",
                                "publisher": "허블",
                                "coverImageUrl": "https://contents.kyobobook.co.kr/sih/fit-in/458x0/pdt/9788954655972.jpg",
                                "kdcName": "한국소설"
                              },
                              "meetingSummary": null
                            }
                          }
                          """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "모임을 찾을 수 없음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "MEETING404_1",
                "message": "해당 모임을 찾을 수 없습니다.",
                "result": null
              }
              """)))
  })
  ResponseEntity<ApiResponse<MeetingDetailResDTO>> getMeetingDetail(
      @Parameter(description = "모임 ID", example = "1004", required = true) Long meetingId);

  @Operation(
      summary = "독서 모임 생성",
      description = "ISBN으로 도서를 조회한 뒤 시작 일시, 최대 인원, 진행 시간을 설정해 독서 모임을 생성합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "모임 생성 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEETING201_1",
                "message": "모임이 생성되었습니다.",
                "result": {
                  "id": 1
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 값 검증 실패",
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
                "result": null
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "도서를 찾을 수 없음",
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
              """)))
  })
  ResponseEntity<ApiResponse<MeetingCreateResDTO>> createMeeting(
      @Parameter(description = "하이픈을 제거한 ISBN13", example = "9788966262281", required = true)
          String isbn,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              description = "생성할 독서 모임 정보입니다.",
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = MeetingCreateReqDTO.class),
                      examples =
                          @ExampleObject(
                              value =
                                  """
              {
                "startDate": "2026-08-01",
                "startTime": "20:00",
                "maxParticipants": 4,
                "duration": 60
              }
              """)))
          MeetingCreateReqDTO request);
}
