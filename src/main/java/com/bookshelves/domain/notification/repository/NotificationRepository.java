package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByMember_Id(Long memberId, Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Notification notification
      set notification.isDelivered = true
      where notification.member.id = :memberId
        and notification.isDelivered = false
        and notification.isOffered = true
        and notification.id <= :afterId
      """)
  int markDeliveredThrough(@Param("memberId") Long memberId, @Param("afterId") Long afterId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update Notification notification
      set notification.isOffered = true
      where notification.member.id = :memberId
        and notification.id in :notificationIds
      """)
  int markOffered(
      @Param("memberId") Long memberId,
      @Param("notificationIds") java.util.List<Long> notificationIds);

  Slice<Notification> findAllByMember_IdAndIsDeliveredFalseOrderByIdAsc(
      Long memberId, Pageable pageable);

  Optional<Notification> findByIdAndMember_Id(Long id, Long memberId);
}
