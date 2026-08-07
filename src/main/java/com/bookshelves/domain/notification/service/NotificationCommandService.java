package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.exception.MemberException;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.code.NotificationErrorCode;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.exception.NotificationException;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final NotificationRepository notificationRepository;
  private final MemberRepository memberRepository;
  private final ApplicationEventPublisher eventPublisher;

  public void registerFcmToken(Long memberId, String fcmToken) {
    deviceTokenRepository.upsertFcmToken(memberId, fcmToken);
  }

  // 같은 회원의 알림은 Member 행 잠금을 획득한 트랜잭션만 INSERT할 수 있다.
  // IDENTITY ID 할당 순서와 커밋 순서를 일치시켜 ID 커서 폴링의 누락을 방지한다.
  // 여러 회원을 함께 처리할 때는 교착 상태를 막기 위해 회원 ID 오름차순으로 잠근다.
  public List<Notification> createNotifications(List<Notification> notifications) {
    List<Long> memberIds =
        notifications.stream()
            .map(notification -> notification.getMember().getId())
            .distinct()
            .sorted()
            .toList();

    memberIds.forEach(
        memberId ->
            memberRepository
                .findByIdForUpdate(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND)));

    List<Notification> savedNotifications = notificationRepository.saveAllAndFlush(notifications);
    if (!savedNotifications.isEmpty()) {
      eventPublisher.publishEvent(NotificationPushEvent.from(savedNotifications));
    }
    return savedNotifications;
  }

  public NotificationReadResponse readNotification(Long notificationId, Long memberId) {
    Notification notification = findOwnedNotification(notificationId, memberId);

    notification.markAsRead();
    return new NotificationReadResponse(notification.getId());
  }

  public void deleteNotification(Long notificationId, Long memberId) {
    Notification notification = findOwnedNotification(notificationId, memberId);
    notification.delete();
  }

  private Notification findOwnedNotification(Long notificationId, Long memberId) {
    return notificationRepository
        .findByIdAndMember_IdAndIsDeletedFalse(notificationId, memberId)
        .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
  }
}
