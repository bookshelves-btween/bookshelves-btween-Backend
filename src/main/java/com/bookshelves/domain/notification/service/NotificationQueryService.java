package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.notification.converter.NotificationConverter;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse;
import com.bookshelves.domain.notification.dto.response.NotificationListResponse.NotificationInfo;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

  private final NotificationRepository notificationRepository;

  public NotificationListResponse getNotifications(Long memberId, int page, int size) {
    PageRequest pageRequest =
        PageRequest.of(
            page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    Page<Notification> notificationPage =
        notificationRepository.findAllByMember_Id(memberId, pageRequest);

    return NotificationConverter.toNotificationListResponse(notificationPage);
  }

  public List<NotificationInfo> getNewNotifications(Long memberId, Long afterId) {
    return notificationRepository
        .findAllByMember_IdAndIdGreaterThanOrderByIdAsc(memberId, afterId)
        .stream()
        .map(NotificationConverter::toNotificationInfo)
        .toList();
  }
}
