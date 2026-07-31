package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import java.util.List;

public record NotificationPushEvent(List<PushNotification> notifications) {

  public static NotificationPushEvent from(List<Notification> notifications) {
    return new NotificationPushEvent(
        notifications.stream()
            .map(
                notification ->
                    new PushNotification(
                        notification.getId(),
                        notification.getMember().getId(),
                        notification.getType(),
                        notification.getTitle(),
                        notification.getContent(),
                        notification.getRelatedId()))
            .toList());
  }

  public record PushNotification(
      Long id, Long memberId, NotificationType type, String title, String content, Long targetId) {}
}
