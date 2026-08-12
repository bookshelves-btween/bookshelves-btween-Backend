package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class NotificationPushListenerTest {

  @Mock private ObjectProvider<FcmNotificationSender> senderProvider;
  @Mock private FcmNotificationSender sender;
  @Mock private ThreadPoolTaskExecutor taskExecutor;
  @Mock private TaskScheduler retryScheduler;

  @Test
  void submitsEachMemberPushAsAnIndependentTask() {
    PushNotification first =
        new PushNotification(1L, 10L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    PushNotification second =
        new PushNotification(2L, 20L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    given(senderProvider.getIfAvailable()).willReturn(sender);
    NotificationPushListener listener =
        new NotificationPushListener(senderProvider, taskExecutor, retryScheduler);

    listener.sendAfterCommit(new NotificationPushEvent(List.of(first, second)));

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskExecutor, times(2)).execute(taskCaptor.capture());
    taskCaptor.getAllValues().forEach(Runnable::run);
    verify(sender).send(first);
    verify(sender).send(second);
  }

  @Test
  void retriesRejectedTaskWithoutSkippingLaterNotificationsOrPropagatingException() {
    PushNotification rejected =
        new PushNotification(1L, 10L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    PushNotification second =
        new PushNotification(2L, 20L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    PushNotification third =
        new PushNotification(3L, 30L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    ScheduledFuture<?> retryFuture = org.mockito.Mockito.mock(ScheduledFuture.class);
    given(senderProvider.getIfAvailable()).willReturn(sender);
    doThrow(new TaskRejectedException("executor full"))
        .doNothing()
        .doNothing()
        .doNothing()
        .when(taskExecutor)
        .execute(any(Runnable.class));
    doReturn(retryFuture).when(retryScheduler).schedule(any(Runnable.class), any(Instant.class));
    NotificationPushListener listener =
        new NotificationPushListener(senderProvider, taskExecutor, retryScheduler);

    assertThatCode(
            () ->
                listener.sendAfterCommit(
                    new NotificationPushEvent(List.of(rejected, second, third))))
        .doesNotThrowAnyException();

    verify(taskExecutor, times(3)).execute(any(Runnable.class));
    ArgumentCaptor<Runnable> retryCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(retryScheduler).schedule(retryCaptor.capture(), any(Instant.class));
    retryCaptor.getValue().run();
    verify(taskExecutor, times(4)).execute(any(Runnable.class));
  }
}
