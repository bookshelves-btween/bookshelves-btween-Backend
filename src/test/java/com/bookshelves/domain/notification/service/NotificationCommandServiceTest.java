package com.bookshelves.domain.notification.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;

class NotificationCommandServiceTest {

  private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
  private final NotificationCommandService notificationCommandService =
      new NotificationCommandService(deviceTokenRepository);

  @Test
  void registerFcmTokenUpsertsTokenAtomically() {
    notificationCommandService.registerFcmToken(1L, "fcm-token");

    verify(deviceTokenRepository).upsertFcmToken(1L, "fcm-token");
  }
}
