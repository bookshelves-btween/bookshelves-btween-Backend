package com.bookshelves.domain.member.entity;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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
}
