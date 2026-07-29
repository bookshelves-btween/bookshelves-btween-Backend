package com.bookshelves.domain.notification.dto.response;

import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "새 알림 커서 조회 결과")
public record NewNotificationResponse(
    @Schema(description = "아직 전달 완료되지 않은 알림 목록") List<NotificationInfo> notifications,
    @Schema(description = "응답을 정상 처리한 후 다음 요청의 afterId로 전달할 커서") Long nextCursor,
    boolean hasNext) {}
