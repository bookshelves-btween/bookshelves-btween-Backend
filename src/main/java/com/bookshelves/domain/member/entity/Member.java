package com.bookshelves.domain.member.entity;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_member_provider_provider_id",
          columnNames = {"provider", "provider_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

  public static final long RESTORE_PERIOD_DAYS = 30;

  // deletedAt은 항상 이 타임존 기준 벽시계 값으로 기록한다.
  // JVM 기본 타임존(LocalDateTime.now())을 쓰면 서버 OS 설정에 따라 값이 달라져,
  // 이후 AuthCommandService/MemberCommandService/MemberAnonymizationScheduler가
  // 이 값을 Asia/Seoul로 재해석할 때 어긋난다.
  public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nickname", length = 50)
  private String nickname;

  @Column(name = "nickname_noun", length = 30)
  private String nicknameNoun;

  @Column(name = "nickname_modifier", length = 30)
  private String nicknameModifier;

  @Column(name = "nickname_animal", length = 30)
  private String nicknameAnimal;

  @Enumerated(EnumType.STRING)
  @Column(name = "profile_background_color")
  private ProfileBackgroundColor profileBackgroundColor;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private MemberStatus status = MemberStatus.PENDING_ONBOARDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider")
  private Provider provider;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public static Member createSocialMember(Provider provider, String providerId) {
    Member member = new Member();
    member.provider = provider;
    member.providerId = providerId;
    return member;
  }

  public void updateNickname(String nicknameNoun, String nicknameModifier, String nicknameAnimal) {
    this.nicknameNoun = nicknameNoun;
    this.nicknameModifier = nicknameModifier;
    this.nicknameAnimal = nicknameAnimal;
    this.nickname = nicknameNoun + " " + nicknameModifier + " " + nicknameAnimal;
  }

  public void updateProfileBackgroundColor(ProfileBackgroundColor profileBackgroundColor) {
    this.profileBackgroundColor = profileBackgroundColor;
  }

  public void withdraw() {
    this.status = MemberStatus.WITHDRAWN;
    this.deletedAt = LocalDateTime.now(SERVICE_ZONE);
  }

  // 탈퇴는 ACTIVE 상태에서만 가능하도록 제한돼 있으므로, 복구 후 상태는 항상 ACTIVE로 되돌린다.
  public void restore() {
    this.status = MemberStatus.ACTIVE;
    this.deletedAt = null;
  }

  // provider/providerId까지 지워야 동일 소셜 계정으로 재가입이 가능해진다.
  public void anonymize() {
    this.status = MemberStatus.ANONYMIZED;
    this.nickname = "탈퇴한 사용자" + this.id;
    this.nicknameNoun = null;
    this.nicknameModifier = null;
    this.nicknameAnimal = null;
    this.provider = null;
    this.providerId = null;
  }
}
