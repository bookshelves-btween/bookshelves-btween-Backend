package com.bookshelves.domain.notification.controller;

import com.bookshelves.domain.notification.code.NotificationSuccessCode;
import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.notification.service.NotificationQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

  private final NotificationCommandService notificationCommandService;
  private final NotificationQueryService notificationQueryService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  public ResponseEntity<ApiResponse<Void>> registerFcmToken(
      @Valid @RequestBody FcmTokenRegisterRequest request) {
    notificationCommandService.registerFcmToken(
        authenticationFacade.getCurrentMemberId(), request.fcmToken());

    return ResponseEntity.ok(
        ApiResponse.onSuccess(NotificationSuccessCode.FCM_TOKEN_REGISTERED, null));
  }

  @Override
  public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @RequestParam(name = "page", defaultValue = "1") Integer page,
      @RequestParam(name = "size", defaultValue = "20") Integer size) {
    NotificationListResponse response =
        notificationQueryService.getNotifications(
            authenticationFacade.getCurrentMemberId(), page, size);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(NotificationSuccessCode.NOTIFICATION_LIST_FOUND, response));
  }

  @Override
  public ResponseEntity<ApiResponse<NotificationReadResponse>> readNotification(
      @PathVariable(name = "notificationId") Long notificationId) {
    NotificationReadResponse response =
        notificationCommandService.readNotification(
            notificationId, authenticationFacade.getCurrentMemberId());

    return ResponseEntity.ok(
        ApiResponse.onSuccess(NotificationSuccessCode.NOTIFICATION_READ, response));
  }
}
