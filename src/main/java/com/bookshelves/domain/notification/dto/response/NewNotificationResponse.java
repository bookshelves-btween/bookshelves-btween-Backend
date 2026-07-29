package com.bookshelves.domain.notification.dto.response;

import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "새 알림 커서 조회 결과")
public record NewNotificationResponse(
    List<NotificationInfo> notifications, Long nextCursor, boolean hasNext) {}
