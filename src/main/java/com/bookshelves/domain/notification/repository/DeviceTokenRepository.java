package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {}
