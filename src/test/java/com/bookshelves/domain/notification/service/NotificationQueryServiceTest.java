package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.dto.response.NewNotificationResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

class NotificationQueryServiceTest {

  private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
  private final NotificationQueryService notificationQueryService =
      new NotificationQueryService(notificationRepository);

  @Test
  void getNotificationsReturnsPagedNotificationsInRequestedShape() {
    Notification notification = mock(Notification.class);
    LocalDateTime createdAt = LocalDateTime.of(2026, 7, 14, 20, 0);
    when(notification.getId()).thenReturn(101L);
    when(notification.getType()).thenReturn(NotificationType.MEETING_STARTED);
    when(notification.getTitle()).thenReturn("모임이 곧 시작됩니다.");
    when(notification.getContent()).thenReturn("10분 후 모임이 시작됩니다.");
    when(notification.getIsRead()).thenReturn(false);
    when(notification.getRelatedId()).thenReturn(12L);
    when(notification.getCreatedAt()).thenReturn(createdAt);

    PageRequest pageRequest =
        PageRequest.of(0, 1, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    when(notificationRepository.findAllByMember_Id(1L, pageRequest))
        .thenReturn(new PageImpl<>(List.of(notification), pageRequest, 2));

    NotificationListResponse response = notificationQueryService.getNotifications(1L, 1, 1);

    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.notifications()).hasSize(1);
    assertThat(response.notifications().getFirst().id()).isEqualTo(101L);
    assertThat(response.notifications().getFirst().type())
        .isEqualTo(NotificationType.MEETING_STARTED);
    assertThat(response.notifications().getFirst().targetId()).isEqualTo(12L);
    assertThat(response.notifications().getFirst().createdAt()).isEqualTo(createdAt);
    verify(notificationRepository).findAllByMember_Id(1L, pageRequest);
  }

  @Test
  void getNotificationsReturnsEmptyListWhenMemberHasNoNotifications() {
    PageRequest pageRequest =
        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    when(notificationRepository.findAllByMember_Id(1L, pageRequest))
        .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

    NotificationListResponse response = notificationQueryService.getNotifications(1L, 1, 20);

    assertThat(response.notifications()).isEmpty();
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  void getNewNotificationsAcknowledgesCursorAndReturnsUndeliveredNotifications() {
    Notification first = mock(Notification.class);
    Notification second = mock(Notification.class);
    when(first.getId()).thenReturn(101L);
    when(first.getType()).thenReturn(NotificationType.MEETING_STARTED);
    when(second.getId()).thenReturn(102L);
    when(second.getType()).thenReturn(NotificationType.SYSTEM);
    PageRequest pageRequest = PageRequest.of(0, 2);
    when(notificationRepository.findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(1L, pageRequest))
        .thenReturn(new SliceImpl<>(List.of(first, second), pageRequest, true));

    NewNotificationResponse response = notificationQueryService.getNewNotifications(1L, 100L, 2);

    assertThat(response.notifications())
        .extracting(NotificationInfo::id)
        .containsExactly(101L, 102L);
    assertThat(response.notifications())
        .extracting(NotificationInfo::type)
        .containsExactly(NotificationType.MEETING_STARTED, NotificationType.SYSTEM);
    assertThat(response.nextCursor()).isEqualTo(102L);
    assertThat(response.hasNext()).isTrue();
    verify(notificationRepository).markDeliveredThrough(1L, 100L);
    verify(notificationRepository).markOffered(1L, List.of(101L, 102L));
    verify(notificationRepository)
        .findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(1L, pageRequest);
  }

  @Test
  void getNewNotificationsKeepsCursorWhenNoUndeliveredNotificationExists() {
    PageRequest pageRequest = PageRequest.of(0, 20);
    when(notificationRepository.findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(1L, pageRequest))
        .thenReturn(new SliceImpl<>(List.of(), pageRequest, false));

    NewNotificationResponse response = notificationQueryService.getNewNotifications(1L, 100L, 20);

    assertThat(response.notifications()).isEmpty();
    assertThat(response.nextCursor()).isEqualTo(100L);
    assertThat(response.hasNext()).isFalse();
    verify(notificationRepository).markDeliveredThrough(1L, 100L);
  }

  @Test
  void getNewNotificationsReturnsDelayedLowerIdBecauseItIsStillUndelivered() {
    Notification lateCommittedNotification = mock(Notification.class);
    when(lateCommittedNotification.getId()).thenReturn(101L);
    when(lateCommittedNotification.getType()).thenReturn(NotificationType.MEETING_STARTED);
    PageRequest pageRequest = PageRequest.of(0, 20);
    when(notificationRepository.findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(1L, pageRequest))
        .thenReturn(new SliceImpl<>(List.of(lateCommittedNotification), pageRequest, false));

    NewNotificationResponse response = notificationQueryService.getNewNotifications(1L, 102L, 20);

    assertThat(response.notifications()).extracting(NotificationInfo::id).containsExactly(101L);
    assertThat(response.nextCursor()).isEqualTo(102L);
    assertThat(response.hasNext()).isFalse();
    verify(notificationRepository).markDeliveredThrough(1L, 102L);
    verify(notificationRepository).markOffered(1L, List.of(101L));
  }

  @Test
  void getNewNotificationsAcknowledgesNothingForInitialCursor() {
    PageRequest pageRequest = PageRequest.of(0, 20);
    when(notificationRepository.findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(1L, pageRequest))
        .thenReturn(new SliceImpl<>(List.of(), pageRequest, false));

    NewNotificationResponse response = notificationQueryService.getNewNotifications(1L, 0L, 20);

    assertThat(response.notifications()).isEmpty();
    assertThat(response.nextCursor()).isZero();
    verify(notificationRepository).markDeliveredThrough(1L, 0L);
  }
}
