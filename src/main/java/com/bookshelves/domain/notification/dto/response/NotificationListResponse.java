package com.bookshelves.domain.notification.dto.response;

import com.bookshelves.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "알림 목록 조회 결과")
public record NotificationListResponse(
    List<NotificationInfo> notifications, Integer page, Integer size, boolean hasNext) {

  @Schema(description = "알림 정보")
  public record NotificationInfo(
      Long id,
      NotificationType type,
      String title,
      String content,
      Boolean isRead,
      Long relatedId,
      LocalDateTime createdAt) {}
}
