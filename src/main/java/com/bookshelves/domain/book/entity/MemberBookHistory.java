package com.bookshelves.domain.book.entity;

import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberBookHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_book_id", nullable = false)
  private MemberBook memberBook;

  @Column(name = "progress", nullable = false)
  private Integer progress;

  public static MemberBookHistory create(MemberBook memberBook, Integer progress) {
    MemberBookHistory memberBookHistory = new MemberBookHistory();
    memberBookHistory.memberBook = memberBook;
    memberBookHistory.progress = progress;
    return memberBookHistory;
  }
}
