package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "external.firebase", name = "enabled", havingValue = "true")
public class FcmNotificationSender {

  private final FirebaseMessaging firebaseMessaging;
  private final DeviceTokenRepository deviceTokenRepository;

  public void send(PushNotification notification) {
    List<String> tokens = deviceTokenRepository.findFcmTokensByMemberId(notification.memberId());
    if (tokens.isEmpty()) {
      return;
    }

    MulticastMessage message = createMessage(notification, tokens);
    try {
      var response = firebaseMessaging.sendEachForMulticast(message);
      if (response.getFailureCount() > 0) {
        log.warn(
            "FCM 일부 발송 실패: notificationId={}, success={}, failure={}",
            notification.id(),
            response.getSuccessCount(),
            response.getFailureCount());
      }
    } catch (FirebaseMessagingException e) {
      log.error("FCM 발송 실패: notificationId={}", notification.id(), e);
    }
  }

  static MulticastMessage createMessage(PushNotification notification, List<String> tokens) {
    FcmPayload payload = createPayload(notification);
    MulticastMessage.Builder builder =
        MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(payload.title())
                    .setBody(payload.body())
                    .build())
            .putAllData(payload.data())
            .setApnsConfig(
                ApnsConfig.builder()
                    .putHeader("apns-priority", "10")
                    .setAps(Aps.builder().setSound("default").build())
                    .build());
    return builder.build();
  }

  static FcmPayload createPayload(PushNotification notification) {
    Map<String, String> data = new LinkedHashMap<>();
    data.put("notificationId", notification.id().toString());
    data.put("type", notification.type().name());
    if (notification.targetId() != null) {
      data.put("targetId", notification.targetId().toString());
    }
    return new FcmPayload(
        notification.title(), notification.content() == null ? "" : notification.content(), data);
  }

  record FcmPayload(String title, String body, Map<String, String> data) {}
}
