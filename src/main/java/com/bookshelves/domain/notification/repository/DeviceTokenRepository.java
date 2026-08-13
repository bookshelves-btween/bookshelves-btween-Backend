package com.bookshelves.domain.notification.repository;

import com.bookshelves.domain.notification.entity.DeviceToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  @Query("select dt.fcmToken from DeviceToken dt where dt.member.id = :memberId order by dt.id")
  List<String> findFcmTokensByMemberId(@Param("memberId") Long memberId);

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

  // 벌크 delete로 즉시 실행되어야 한다. member_category와 동일한 이유로, derived delete는
  // 같은 트랜잭션에서 뒤이은 쓰기와 flush 순서가 어긋날 수 있다.
  @Modifying
  @Query("DELETE FROM DeviceToken dt WHERE dt.member.id = :memberId")
  void deleteByMember_Id(@Param("memberId") Long memberId);
}
