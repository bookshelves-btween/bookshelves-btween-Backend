package com.bookshelves.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequest(
    @Schema(
            description = "iOS 디바이스에서 발급받은 FCM 등록 토큰",
            example = "eYx7K9mQ2Vw:APA91bFcmDeviceTokenExample")
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Size(max = 255, message = "FCM 토큰은 255자 이하여야 합니다.")
        String fcmToken) {}
