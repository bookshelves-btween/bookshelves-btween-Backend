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

  // 읽음 처리와 soft delete가 동시에 stale 상태를 저장해 삭제를 되돌리지 않도록 행 변경을 직렬화한다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Notification> findByIdAndMember_IdAndIsDeletedFalse(Long id, Long memberId);

  // 요약 완료 알림은 중복 이벤트로 두 번 생성될 수 있다. 회원·타입·모임 조합으로 존재를 확인한다.
  boolean existsByMember_IdAndTypeAndRelatedId(
      Long memberId, NotificationType type, Long relatedId);
}
