package com.bookshelves.domain.meeting.controller;

import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO;
import com.bookshelves.domain.meeting.exception.code.MeetingSuccessCode;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.domain.meeting.service.MeetingQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingController implements MeetingControllerDocs {

  private final MeetingCommandService meetingCommandService;
  private final MeetingQueryService meetingQueryService;

  @Override
  @GetMapping("/api/v1/meetings")
  public ResponseEntity<ApiResponse<MeetingSearchResDTO>> searchMeetings(
      @RequestParam String name,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    MeetingSearchResDTO response = meetingQueryService.searchMeetings(name, page, size);
    return ResponseEntity.ok(
        ApiResponse.onSuccess(MeetingSuccessCode.MEETING_LIST_FOUND, response));
  }

  @Override
  @GetMapping("/api/v1/meetings/{meetingId}")
  public ResponseEntity<ApiResponse<MeetingDetailResDTO>> getMeetingDetail(
      @PathVariable Long meetingId) {
    MeetingDetailResDTO response = meetingQueryService.getMeetingDetail(meetingId);
    return ResponseEntity.ok(
        ApiResponse.onSuccess(MeetingSuccessCode.MEETING_DETAIL_FOUND, response));
  }

  @Override
  @PostMapping("/api/v1/{isbn}/recruitment")
  public ResponseEntity<ApiResponse<MeetingCreateResDTO>> createMeeting(
      @PathVariable String isbn, @RequestBody MeetingCreateReqDTO request) {
    MeetingCreateResDTO response = meetingCommandService.createMeeting(isbn, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.onSuccess(MeetingSuccessCode.MEETING_CREATED, response));
  }
}
