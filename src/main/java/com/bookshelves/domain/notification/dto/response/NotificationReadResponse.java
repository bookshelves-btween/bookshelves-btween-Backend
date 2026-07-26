package com.bookshelves.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 읽음 처리 결과")
public record NotificationReadResponse(
    @Schema(description = "읽음 처리된 알림 ID", example = "101") Long id) {}
