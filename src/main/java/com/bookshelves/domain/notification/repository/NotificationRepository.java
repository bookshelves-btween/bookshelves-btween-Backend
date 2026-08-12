package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByMember_IdAndIsDeletedFalse(Long memberId, Pageable pageable);

  Slice<Notification> findAllByMember_IdAndIsDeletedFalseAndIdGreaterThanOrderByIdAsc(
      Long memberId, Long afterId, Pageable pageable);

  // 읽음 처리와 soft delete의 동시 갱신을 직렬화한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Notification> findByIdAndMember_IdAndIsDeletedFalse(Long id, Long memberId);

  // 회원·타입·모임 조합으로 요약 완료 알림의 중복을 확인한다.
  boolean existsByMember_IdAndTypeAndRelatedId(
      Long memberId, NotificationType type, Long relatedId);
}
