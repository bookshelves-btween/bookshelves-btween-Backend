package com.bookshelves.domain.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequest(
    @Schema(
            description = "iOS 디바이스에서 발급받은 FCM 등록 토큰",
            example = "eYx7K9mQ2Vw:APA91bFcmDeviceTokenExample")
        @NotBlank
        @Size(max = 255)
        String fcmToken) {}
