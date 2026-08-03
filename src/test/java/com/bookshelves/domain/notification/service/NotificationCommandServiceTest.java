package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.code.NotificationErrorCode;
import com.bookshelves.domain.notification.dto.response.NotificationReadResponse;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.exception.NotificationException;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

class NotificationCommandServiceTest {

  private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
  private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
  private final NotificationCommandService notificationCommandService =
      new NotificationCommandService(
          deviceTokenRepository, notificationRepository, memberRepository, eventPublisher);

  @Test
  void registerFcmTokenUpsertsTokenAtomically() {
    notificationCommandService.registerFcmToken(1L, "fcm-token");

    verify(deviceTokenRepository).upsertFcmToken(1L, "fcm-token");
  }

  @Test
  void createNotificationsLocksRecipientMembersInIdOrderBeforeAllocatingNotificationIds() {
    Notification forSecondMember = mock(Notification.class);
    Notification forFirstMember = mock(Notification.class);
    Member firstMember = mock(Member.class);
    Member secondMember = mock(Member.class);
    when(firstMember.getId()).thenReturn(1L);
    when(secondMember.getId()).thenReturn(2L);
    when(forSecondMember.getMember()).thenReturn(secondMember);
    when(forFirstMember.getMember()).thenReturn(firstMember);
    when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(firstMember));
    when(memberRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(secondMember));
    List<Notification> notifications = List.of(forSecondMember, forFirstMember);
    when(notificationRepository.saveAllAndFlush(notifications)).thenReturn(notifications);

    List<Notification> saved = notificationCommandService.createNotifications(notifications);

    assertThat(saved).isSameAs(notifications);
    InOrder order = inOrder(memberRepository, notificationRepository);
    order.verify(memberRepository).findByIdForUpdate(1L);
    order.verify(memberRepository).findByIdForUpdate(2L);
    order.verify(notificationRepository).saveAllAndFlush(notifications);
    verify(eventPublisher).publishEvent(NotificationPushEvent.from(notifications));
  }

  @Test
  void readNotificationMarksOwnedNotificationAsRead() {
    Notification notification = mock(Notification.class);
    when(notification.getId()).thenReturn(101L);
    when(notificationRepository.findByIdAndMember_Id(101L, 1L))
        .thenReturn(Optional.of(notification));

    NotificationReadResponse response = notificationCommandService.readNotification(101L, 1L);

    assertThat(response.id()).isEqualTo(101L);
    verify(notification).markAsRead();
  }

  @Test
  void readNotificationThrowsNotFoundWhenNotificationIsMissingOrNotOwned() {
    when(notificationRepository.findByIdAndMember_Id(101L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationCommandService.readNotification(101L, 1L))
        .isInstanceOf(NotificationException.class)
        .extracting(exception -> ((NotificationException) exception).getErrorCode())
        .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
  }
}
