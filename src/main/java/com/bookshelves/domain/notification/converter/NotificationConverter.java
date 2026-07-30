package com.bookshelves.domain.notification.converter;

import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import com.bookshelves.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;

public final class NotificationConverter {

  private NotificationConverter() {}

  public static NotificationListResponse toNotificationListResponse(
      Page<Notification> notificationPage) {
    List<NotificationInfo> notifications =
        notificationPage.getContent().stream()
            .map(NotificationConverter::toNotificationInfo)
            .toList();

    return new NotificationListResponse(
        notifications,
        notificationPage.getNumber() + 1,
        notificationPage.getSize(),
        notificationPage.hasNext());
  }

  public static NotificationInfo toNotificationInfo(Notification notification) {
    return new NotificationInfo(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getContent(),
        notification.getIsRead(),
        notification.getRelatedId(),
        notification.getCreatedAt());
  }
}
