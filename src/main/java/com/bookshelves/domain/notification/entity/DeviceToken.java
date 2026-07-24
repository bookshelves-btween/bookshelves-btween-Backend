package com.bookshelves.domain.notification.entity;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.notification.enums.Platform;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_device_token_fcm_token",
          columnNames = {"fcm_token"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "fcm_token", nullable = false, length = 255)
  private String fcmToken;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform", nullable = false)
  private Platform platform = Platform.IOS;

  public static DeviceToken create(Member member, String fcmToken) {
    DeviceToken deviceToken = new DeviceToken();
    deviceToken.member = member;
    deviceToken.fcmToken = fcmToken;
    return deviceToken;
  }

  public void assignTo(Member member) {
    this.member = member;
  }
}
