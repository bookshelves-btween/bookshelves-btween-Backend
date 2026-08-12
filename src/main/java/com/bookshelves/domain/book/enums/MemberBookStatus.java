package com.bookshelves.domain.book.enums;

public enum MemberBookStatus {
  ALL,
  BEFORE_READING,
  READING,
  FINISHED;

  // 상태는 별도 저장하지 않고 진행률에서 파생한다.
  public static MemberBookStatus from(int progress) {
    if (progress == 0) {
      return BEFORE_READING;
    }
    if (progress == 100) {
      return FINISHED;
    }
    return READING;
  }
}
