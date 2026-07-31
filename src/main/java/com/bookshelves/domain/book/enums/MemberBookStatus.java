package com.bookshelves.domain.book.enums;

public enum MemberBookStatus {
  ALL,
  BEFORE_READING,
  READING,
  FINISHED;

  // member_book에는 status 컬럼이 없다. 진행률이 유일한 원본이고 상태는 거기서 파생한다.
  // 서재 목록과 홈이 같은 규칙을 써야 하므로 파생을 enum에 둔다.
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
