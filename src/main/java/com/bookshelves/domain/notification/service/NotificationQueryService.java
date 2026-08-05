package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.converter.NotificationConverter;
import com.bookshelves.domain.notification.dto.response.NewNotificationResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

  private final NotificationRepository notificationRepository;

  @Transactional(readOnly = true)
  public NotificationListResponse getNotifications(Long memberId, int page, int size) {
    PageRequest pageRequest =
        PageRequest.of(
            page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    Page<Notification> notificationPage =
        notificationRepository.findAllByMember_IdAndIsDeletedFalse(memberId, pageRequest);

    return NotificationConverter.toNotificationListResponse(notificationPage);
  }

  @Transactional(readOnly = true)
  public NewNotificationResponse getNewNotifications(Long memberId, Long afterId, int size) {
    PageRequest pageRequest = PageRequest.of(0, size);
    Slice<Notification> notificationSlice =
        notificationRepository.findAllByMember_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
            memberId, afterId, pageRequest);
    List<NotificationInfo> notifications =
        notificationSlice.stream().map(NotificationConverter::toNotificationInfo).toList();
    Long nextCursor = notifications.isEmpty() ? afterId : notifications.getLast().id();

    return new NewNotificationResponse(notifications, nextCursor, notificationSlice.hasNext());
  }
}
