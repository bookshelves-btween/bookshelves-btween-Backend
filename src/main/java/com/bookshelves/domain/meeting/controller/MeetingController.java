package com.bookshelves.domain.meeting.controller;

import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingCreateResDTO;
import com.bookshelves.domain.meeting.exception.code.MeetingSuccessCode;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import com.bookshelves.global.exception.ProjectException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeetingController implements MeetingControllerDocs {

  private final MeetingCommandService meetingCommandService;

  @Override
  @PostMapping("/api/v1/{isbn}/recruitment")
  public ResponseEntity<ApiResponse<MeetingCreateResDTO>> createMeeting(
      @PathVariable String isbn,
      @Valid @RequestBody MeetingCreateReqDTO request,
      BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
    }

    MeetingCreateResDTO response = meetingCommandService.createMeeting(isbn, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.onSuccess(MeetingSuccessCode.MEETING_CREATED, response));
  }
}
