package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
}
