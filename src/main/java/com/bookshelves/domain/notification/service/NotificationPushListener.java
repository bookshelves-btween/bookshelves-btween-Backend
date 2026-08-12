package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.service.NotificationPushEvent.PushNotification;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class NotificationPushListener {

  private static final int MAX_SUBMISSION_ATTEMPTS = 4;
  private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

  private final ObjectProvider<FcmNotificationSender> senderProvider;
  private final ThreadPoolTaskExecutor notificationPushTaskExecutor;
  private final TaskScheduler notificationPushRetryScheduler;

  public NotificationPushListener(
      ObjectProvider<FcmNotificationSender> senderProvider,
      @Qualifier("notificationPushTaskExecutor")
          ThreadPoolTaskExecutor notificationPushTaskExecutor,
      @Qualifier("notificationPushRetryScheduler") TaskScheduler notificationPushRetryScheduler) {
    this.senderProvider = senderProvider;
    this.notificationPushTaskExecutor = notificationPushTaskExecutor;
    this.notificationPushRetryScheduler = notificationPushRetryScheduler;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendAfterCommit(NotificationPushEvent event) {
    FcmNotificationSender sender = senderProvider.getIfAvailable();
    if (sender == null) {
      return;
    }
    event.notifications().forEach(notification -> submit(notification, sender, 1));
  }

  private void submit(PushNotification notification, FcmNotificationSender sender, int attempt) {
    try {
      notificationPushTaskExecutor.execute(() -> sender.send(notification));
    } catch (TaskRejectedException e) {
      // 알림 하나의 거부가 뒤에 있는 알림 제출이나 AFTER_COMMIT 호출자에게 전파되지 않게 격리한다.
      scheduleRetry(notification, sender, attempt, e);
    }
  }

  private void scheduleRetry(
      PushNotification notification,
      FcmNotificationSender sender,
      int attempt,
      TaskRejectedException rejection) {
    if (attempt >= MAX_SUBMISSION_ATTEMPTS) {
      log.error(
          "FCM 작업 제출 최종 실패: notificationId={}, memberId={}, attempts={}",
          notification.id(),
          notification.memberId(),
          attempt,
          rejection);
      return;
    }

    Instant retryAt = Instant.now().plus(RETRY_DELAY.multipliedBy(attempt));
    try {
      ScheduledFuture<?> retryTask =
          notificationPushRetryScheduler.schedule(
              () -> submit(notification, sender, attempt + 1), retryAt);
      if (retryTask == null) {
        logRetryRegistrationFailure(notification, attempt, rejection);
      }
    } catch (RuntimeException retryRegistrationFailure) {
      logRetryRegistrationFailure(notification, attempt, retryRegistrationFailure);
    }
  }

  private void logRetryRegistrationFailure(
      PushNotification notification, int attempt, Throwable cause) {
    log.error(
        "FCM 작업 재시도 등록 실패: notificationId={}, memberId={}, attempts={}",
        notification.id(),
        notification.memberId(),
        attempt,
        cause);
  }
}
