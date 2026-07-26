package com.bookshelves.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.notification.service.NotificationQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NotificationControllerTest {

  private final NotificationCommandService notificationCommandService =
      mock(NotificationCommandService.class);
  private final NotificationQueryService notificationQueryService =
      mock(NotificationQueryService.class);
  private final AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
  private final NotificationController notificationController =
      new NotificationController(
          notificationCommandService, notificationQueryService, authenticationFacade);

  @Test
  void registerFcmTokenReturnsSpecifiedSuccessResponse() {
    when(authenticationFacade.getCurrentMemberId()).thenReturn(1L);

    ResponseEntity<ApiResponse<Void>> response =
        notificationController.registerFcmToken(new FcmTokenRegisterRequest("fcm-token"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getCode()).isEqualTo("NOTI200_1");
    assertThat(response.getBody().getMessage()).isEqualTo("FCM 토큰이 등록되었습니다.");
    assertThat(response.getBody().getResult()).isNull();
    verify(notificationCommandService).registerFcmToken(1L, "fcm-token");
  }

  @Test
  void getNotificationsReturnsSpecifiedSuccessResponse() {
    NotificationListResponse result =
        new NotificationListResponse(java.util.List.of(), 1, 20, false);
    when(authenticationFacade.getCurrentMemberId()).thenReturn(1L);
    when(notificationQueryService.getNotifications(1L, 1, 20)).thenReturn(result);

    ResponseEntity<ApiResponse<NotificationListResponse>> response =
        notificationController.getNotifications(1, 20);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getCode()).isEqualTo("NOTI200_2");
    assertThat(response.getBody().getMessage()).isEqualTo("알림 목록 조회에 성공했습니다.");
    assertThat(response.getBody().getResult()).isSameAs(result);
    verify(notificationQueryService).getNotifications(1L, 1, 20);
  }

  @Test
  void readNotificationReturnsSpecifiedSuccessResponse() {
    NotificationReadResponse result = new NotificationReadResponse(101L);
    when(authenticationFacade.getCurrentMemberId()).thenReturn(1L);
    when(notificationCommandService.readNotification(101L, 1L)).thenReturn(result);

    ResponseEntity<ApiResponse<NotificationReadResponse>> response =
        notificationController.readNotification(101L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getCode()).isEqualTo("NOTI200_3");
    assertThat(response.getBody().getMessage()).isEqualTo("알림을 읽음 처리했습니다.");
    assertThat(response.getBody().getResult()).isSameAs(result);
    verify(notificationCommandService).readNotification(101L, 1L);
  }
}
