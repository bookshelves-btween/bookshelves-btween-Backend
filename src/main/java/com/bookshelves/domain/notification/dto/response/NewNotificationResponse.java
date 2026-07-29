package com.bookshelves.domain.notification.dto.response;

import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "새 알림 커서 조회 결과")
public record NewNotificationResponse(
    @Schema(description = "새 알림과 커밋 순서 역전 방지를 위해 재전송된 알림 목록") List<NotificationInfo> notifications,
    @Schema(description = "새로 확인된 가장 큰 알림 ID. 재전송 알림만 있으면 요청 afterId를 유지") Long nextCursor,
    boolean hasNext) {}
