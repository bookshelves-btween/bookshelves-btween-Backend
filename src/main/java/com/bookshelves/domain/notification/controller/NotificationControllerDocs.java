package com.bookshelves.domain.notification.controller;

import com.bookshelves.domain.notification.dto.request.FcmTokenRegisterRequest;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "알림", description = "알림 API")
public interface NotificationControllerDocs {

  @Operation(
    summary = "FCM 디바이스 토큰 등록",
    description = "인증된 사용자의 FCM 토큰을 등록합니다. MVP에서는 플랫폼을 IOS로 저장합니다.")
  @PostMapping("/api/v1/notifications/fcm/tokens")
  ResponseEntity<ApiResponse<Void>> registerFcmToken(
    @Valid @RequestBody FcmTokenRegisterRequest request);
}
