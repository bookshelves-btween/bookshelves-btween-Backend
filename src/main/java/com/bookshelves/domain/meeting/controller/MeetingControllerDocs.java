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
        schema = @Schema(implementation = MeetingDetailResDTO.class))),
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
    @Parameter(description = "모임 ID", example = "1", required = true) Long meetingId);

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
