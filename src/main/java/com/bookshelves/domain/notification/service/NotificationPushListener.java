package com.bookshelves.domain.notification.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationPushListener {

  private final ObjectProvider<FcmNotificationSender> senderProvider;
  private final ThreadPoolTaskExecutor notificationPushTaskExecutor;

  public NotificationPushListener(
      ObjectProvider<FcmNotificationSender> senderProvider,
      @Qualifier("notificationPushTaskExecutor")
          ThreadPoolTaskExecutor notificationPushTaskExecutor) {
    this.senderProvider = senderProvider;
    this.notificationPushTaskExecutor = notificationPushTaskExecutor;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAfterCommit(NotificationPushEvent event) {
    FcmNotificationSender sender = senderProvider.getIfAvailable();
    if (sender == null) {
      return;
    }
    notificationPushTaskExecutor.execute(() -> event.notifications().forEach(sender::send));
  }
}
