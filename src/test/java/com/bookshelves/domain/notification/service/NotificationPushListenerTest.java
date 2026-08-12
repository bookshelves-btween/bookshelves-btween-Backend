package com.bookshelves.domain.notification.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class NotificationPushListenerTest {

  @Mock private ObjectProvider<FcmNotificationSender> senderProvider;
  @Mock private FcmNotificationSender sender;
  @Mock private ThreadPoolTaskExecutor taskExecutor;

  @Test
  void submitsEachMemberPushAsAnIndependentTask() {
    PushNotification first =
        new PushNotification(1L, 10L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    PushNotification second =
        new PushNotification(2L, 20L, NotificationType.MEETING_STARTED, "title", "body", 100L);
    given(senderProvider.getIfAvailable()).willReturn(sender);
    NotificationPushListener listener = new NotificationPushListener(senderProvider, taskExecutor);

    listener.sendAfterCommit(new NotificationPushEvent(List.of(first, second)));

    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(taskExecutor, times(2)).execute(taskCaptor.capture());
    taskCaptor.getAllValues().forEach(Runnable::run);
    verify(sender).send(first);
    verify(sender).send(second);
  }
}
