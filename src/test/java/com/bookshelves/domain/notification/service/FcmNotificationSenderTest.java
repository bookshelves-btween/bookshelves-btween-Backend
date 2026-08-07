package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;
import java.util.stream.IntStream;
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

  @Test
  void sendsFiveHundredAndOneTokensInTwoMulticastBatches() throws Exception {
    FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
    DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    FcmNotificationSender sender =
        new FcmNotificationSender(firebaseMessaging, deviceTokenRepository);
    PushNotification notification =
        new PushNotification(
            104L, 1L, NotificationType.MEETING_STARTED, "모임이 시작되었어요", "지금 참여해보세요", 12L);
    List<String> tokens = IntStream.range(0, 501).mapToObj(index -> "token-" + index).toList();
    BatchResponse response = mock(BatchResponse.class);
    when(deviceTokenRepository.findFcmTokensByMemberId(1L)).thenReturn(tokens);
    when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(response);

    sender.send(notification);

    assertThat(FcmNotificationSender.partitionTokens(tokens))
        .extracting(List::size)
        .containsExactly(500, 1);
    verify(firebaseMessaging, times(2)).sendEachForMulticast(any(MulticastMessage.class));
  }
}
