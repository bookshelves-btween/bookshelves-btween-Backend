package com.bookshelves.domain.member.entity;

import com.bookshelves.domain.book.entity.Category;
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
          name = "uk_member_category_member_category",
          columnNames = {"member_id", "category_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCategory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  public static MemberCategory create(Member member, Category category) {
    MemberCategory memberCategory = new MemberCategory();
    memberCategory.member = member;
    memberCategory.category = category;
    return memberCategory;
  }
}
