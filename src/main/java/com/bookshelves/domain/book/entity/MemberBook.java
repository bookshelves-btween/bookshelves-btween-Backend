package com.bookshelves.domain.book.entity;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.global.entity.BaseEntity;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_member_book_member_book",
          columnNames = {"member_id", "book_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBook extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_id", nullable = false)
  private Member member;

  @Column(name = "progress", nullable = false)
  private Integer progress = 0;

  @Column(name = "rating", precision = 2, scale = 1)
  private BigDecimal rating;

  @Column(name = "memo", columnDefinition = "TEXT")
  private String memo;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "finished_at")
  private LocalDateTime finishedAt;

  public static MemberBook create(
      Book book, Member member, Integer progress, BigDecimal rating, String memo) {
    MemberBook memberBook = new MemberBook();
    memberBook.book = book;
    memberBook.member = member;
    memberBook.update(progress, rating, memo);
    return memberBook;
  }

  public void update(Integer progress, BigDecimal rating, String memo) {
    this.progress = progress;
    this.rating = rating;
    this.memo = memo;

    if (progress == 0) {
      startedAt = null;
      finishedAt = null;
      return;
    }

    if (startedAt == null) {
      startedAt = LocalDateTime.now();
    }
    finishedAt = progress == 100 ? LocalDateTime.now() : null;
  }
}
