package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import org.junit.jupiter.api.Test;

class FcmNotificationSenderTest {

  @Test
  void fcmPayloadUsesSameNotificationIdAsPollingResponse() {
    PushNotification notification =
        new PushNotification(
            102L, 1L, NotificationType.MEETING_STARTED, "모임이 시작되었어요", "지금 참여해보세요", 12L);

    var payload = FcmNotificationSender.createPayload(notification);

    assertThat(payload.data())
        .containsEntry("notificationId", "102")
        .containsEntry("type", "MEETING_STARTED")
        .containsEntry("targetId", "12");
    assertThat(payload.title()).isEqualTo("모임이 시작되었어요");
    assertThat(payload.body()).isEqualTo("지금 참여해보세요");
  }

  @Test
  void fcmPayloadOmitsTargetIdWhenNotificationHasNoNavigationTarget() {
    PushNotification notification =
        new PushNotification(103L, 1L, NotificationType.MEETING_CANCELED, "모임이 취소되었어요", null, null);

    var payload = FcmNotificationSender.createPayload(notification);

    assertThat(payload.data()).containsEntry("notificationId", "103");
    assertThat(payload.data()).doesNotContainKey("targetId");
    assertThat(payload.body()).isEmpty();
  }
}
