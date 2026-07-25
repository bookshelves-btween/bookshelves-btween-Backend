package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

  private final DeviceTokenRepository deviceTokenRepository;

  public void registerFcmToken(Long memberId, String fcmToken) {
    deviceTokenRepository.upsertFcmToken(memberId, fcmToken);
  }
}
