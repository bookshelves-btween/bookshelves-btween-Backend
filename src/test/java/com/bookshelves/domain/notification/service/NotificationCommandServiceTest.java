package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.code.NotificationErrorCode;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.exception.NotificationException;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NotificationCommandServiceTest {

  private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
  private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
  private final NotificationCommandService notificationCommandService =
      new NotificationCommandService(deviceTokenRepository, notificationRepository);

  @Test
  void registerFcmTokenUpsertsTokenAtomically() {
    notificationCommandService.registerFcmToken(1L, "fcm-token");

    verify(deviceTokenRepository).upsertFcmToken(1L, "fcm-token");
  }

  @Test
  void readNotificationMarksOwnedNotificationAsRead() {
    Notification notification = mock(Notification.class);
    when(notification.getId()).thenReturn(101L);
    when(notificationRepository.findByIdAndMember_Id(101L, 1L))
        .thenReturn(Optional.of(notification));

    NotificationReadResponse response = notificationCommandService.readNotification(101L, 1L);

    assertThat(response.id()).isEqualTo(101L);
    verify(notification).markAsRead();
  }

  @Test
  void readNotificationThrowsNotFoundWhenNotificationIsMissingOrNotOwned() {
    when(notificationRepository.findByIdAndMember_Id(101L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationCommandService.readNotification(101L, 1L))
        .isInstanceOf(NotificationException.class)
        .extracting(exception -> ((NotificationException) exception).getErrorCode())
        .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
  }
}
