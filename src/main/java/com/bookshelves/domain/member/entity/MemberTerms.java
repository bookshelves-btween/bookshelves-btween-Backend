package com.bookshelves.domain.member.entity;

import com.bookshelves.global.entity.BaseEntity;
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
          name = "uk_member_terms_member_terms",
          columnNames = {"member_id", "terms_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTerms extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "terms_id", nullable = false)
  private Terms terms;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  public static MemberTerms create(Member member, Terms terms) {
    MemberTerms memberTerms = new MemberTerms();
    memberTerms.member = member;
    memberTerms.terms = terms;
    return memberTerms;
  }
}
