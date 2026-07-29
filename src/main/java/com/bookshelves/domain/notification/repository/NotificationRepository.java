package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByMember_Id(Long memberId, Pageable pageable);

  Slice<Notification> findAllByMember_IdAndIdGreaterThanOrderByIdAsc(
      Long memberId, Long afterId, Pageable pageable);

  Optional<Notification> findByIdAndMember_Id(Long id, Long memberId);
}
