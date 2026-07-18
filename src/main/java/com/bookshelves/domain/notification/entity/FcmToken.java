package com.bookshelves.domain.notification.entity;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.global.entity.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
          name = "uk_fcm_token_token",
          columnNames = {"token"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends CreatedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "token", nullable = false, length = 512)
  private String token;
}
