package com.bookshelves.domain.notification.controller;

import com.bookshelves.domain.notification.code.NotificationSuccessCode;
import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

  private final NotificationCommandService notificationCommandService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  public ResponseEntity<ApiResponse<Void>> registerFcmToken(
    @Valid @RequestBody FcmTokenRegisterRequest request) {
    notificationCommandService.registerFcmToken(
      authenticationFacade.getCurrentMemberId(), request.fcmToken());

    return ResponseEntity.ok(
      ApiResponse.onSuccess(NotificationSuccessCode.FCM_TOKEN_REGISTERED, null));
  }
}
