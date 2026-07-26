package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.code.NotificationErrorCode;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.exception.NotificationException;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final NotificationRepository notificationRepository;

  public void registerFcmToken(Long memberId, String fcmToken) {
    deviceTokenRepository.upsertFcmToken(memberId, fcmToken);
  }

  public NotificationReadResponse readNotification(Long notificationId, Long memberId) {
    Notification notification =
        notificationRepository
            .findByIdAndMember_Id(notificationId, memberId)
            .orElseThrow(
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

    notification.markAsRead();
    return new NotificationReadResponse(notification.getId());
  }
}
