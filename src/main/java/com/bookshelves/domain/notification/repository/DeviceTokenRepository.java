package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  @Modifying
  @Query(
      value =
          """
          INSERT INTO device_token (member_id, fcm_token, platform, created_at, updated_at)
          VALUES (:memberId, :fcmToken, 'IOS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            member_id = VALUES(member_id),
            platform = VALUES(platform),
            updated_at = CURRENT_TIMESTAMP
          """,
      nativeQuery = true)
  void upsertFcmToken(@Param("memberId") Long memberId, @Param("fcmToken") String fcmToken);
}
