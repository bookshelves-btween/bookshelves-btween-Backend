package com.bookshelves.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NotificationControllerTest {

  private final NotificationCommandService notificationCommandService =
    mock(NotificationCommandService.class);
  private final AuthenticationFacade authenticationFacade = mock(AuthenticationFacade.class);
  private final NotificationController notificationController =
    new NotificationController(notificationCommandService, authenticationFacade);

  @Test
  void registerFcmTokenReturnsSpecifiedSuccessResponse() {
    when(authenticationFacade.getCurrentMemberId()).thenReturn(1L);

    ResponseEntity<ApiResponse<Void>> response =
      notificationController.registerFcmToken(new FcmTokenRegisterRequest("fcm-token"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getCode()).isEqualTo("NOTI200");
    assertThat(response.getBody().getMessage()).isEqualTo("FCM 토큰이 등록되었습니다.");
    assertThat(response.getBody().getResult()).isNull();
    verify(notificationCommandService).registerFcmToken(1L, "fcm-token");
  }
}
