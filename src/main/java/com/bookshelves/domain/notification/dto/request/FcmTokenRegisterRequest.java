package com.bookshelves.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequest(@NotBlank @Size(max = 255) String fcmToken) {
}
