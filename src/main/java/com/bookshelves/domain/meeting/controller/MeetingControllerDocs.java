package com.bookshelves.domain.meeting.controller;

import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingParticipationResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;

@Tag(name = "모임", description = "독서모임 API")
public interface MeetingControllerDocs {

  @Operation(summary = "내 모임 목록 조회", description = "인증된 사용자가 만든 모임 또는 참여한 모임을 연도와 월로 필터링해 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "내 모임 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        name = "내 모임 목록 조회 성공",
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEETING200_4",
                "message": "내 모임 목록 조회에 성공했습니다.",
                "result": {
                  "meetings": [
                    {
                      "id": 1,
                      "chatroomId": 10,
                      "status": "RECRUITING",
                      "startDate": "2026-07-15T19:30:00",
                      "currentParticipants": 7,
                      "maxParticipants": 10,
                      "duration": 90,
                      "book": {
                        "id": 101,
                        "title": "아몬드",
                        "coverImageUrl": "https://image.example.com/almond.jpg"
                      }
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
        description = "필수 파라미터 누락, 요청 값 검증 실패 또는 연도 없이 월만 요청",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "요청 값 검증 실패",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {}
              }
              """),
                  @ExampleObject(
                      name = "연도 없이 월만 요청",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "MEETING400_2",
                "message": "연도가 전체이면 월도 전체여야 합니다.",
                "result": null
              }
              """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 정보가 유효하지 않음",
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
  ResponseEntity<ApiResponse<MeetingSearchResDTO>> getMyMeetings(
      @Parameter(description = "주최자 여부", example = "true", required = true) boolean isLeader,
      @Parameter(description = "조회할 연도. 생략한 경우 월도 생략해야 합니다.", example = "2026")
          @Min(value = 1, message = "연도는 1 이상이어야 합니다.")
          Integer year,
      @Parameter(description = "조회할 월. 연도와 함께 전달한 경우에만 적용됩니다.", example = "7")
          @Min(value = 1, message = "월은 1 이상이어야 합니다.")
          @Max(value = 12, message = "월은 12 이하여야 합니다.")
          Integer month,
      @Parameter(description = "페이지 번호(1부터 시작)", example = "1")
          @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
          Integer page,
      @Parameter(description = "페이지 크기", example = "20")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
          Integer size);

  @Operation(summary = "모임 검색", description = "도서명으로 모임을 검색하고 페이지 단위로 조회합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "모임 목록 조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEETING200_2",
                "message": "모임 목록 조회에 성공했습니다.",
                "result": {
                  "meetings": [
                    {
                      "id": 1001,
                      "chatroomId": 1001,
                      "status": "RECRUITING",
                      "startDate": "2026-08-01T20:00:00",
                      "currentParticipants": 3,
                      "maxParticipants": 4,
                      "duration": 60,
                      "book": {
                        "id": 1002,
                        "title": "우리가 빛의 속도로 갈 수 없다면",
                        "coverImageUrl": "https://contents.kyobobook.co.kr/sih/fit-in/458x0/pdt/9788954655972.jpg"
                      }
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
        description = "필수 검색어 누락 또는 페이지 요청 값 검증 실패",
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
        description = "인증 필요",
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
  ResponseEntity<ApiResponse<MeetingSearchResDTO>> searchMeetings(
      @Parameter(description = "검색할 도서명", example = "혼모노", required = true)
          @NotBlank(message = "도서명은 필수입니다.")
          String name,
      @Parameter(description = "페이지 번호(1부터 시작)", example = "1")
          @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
          Integer page,
      @Parameter(description = "페이지 크기", example = "20")
          @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
          @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
          Integer size);

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
          @Valid
          MeetingCreateReqDTO request);

  @Operation(summary = "모임 참여", description = "인증된 사용자가 독서 모임에 참여합니다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "모임 참여 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "MEETING200_3",
                "message": "모임 요청 성공했습니다.",
                "result": {
                  "meetingParticipantId": 1
                }
              }
              """))),
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
                "result": {}
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "이미 참여한 모임이거나 모집 중인 모임이 아님",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "중복 참여",
                      value =
                          """
                {
                  "isSuccess": false,
                  "code": "MEETING409_2",
                  "message": "이미 참여한 모임입니다.",
                  "result": {}
                }
                """),
                  @ExampleObject(
                      name = "모집 마감",
                      value =
                          """
                {
                  "isSuccess": false,
                  "code": "MEETING409_1",
                  "message": "모집이 마감된 모임입니다.",
                  "result": {}
                }
                """)
                }))
  })
  ResponseEntity<ApiResponse<MeetingParticipationResDTO>> participate(
      @Parameter(description = "모임 ID", example = "1", required = true) Long meetingId);

}
