package com.bookshelves.global.util;

public final class TextTruncator {

  private TextTruncator() {}

  public static String truncate(String value, int maxCodePoints) {
    if (value == null) {
      return null;
    }
    if (maxCodePoints < 0) {
      throw new IllegalArgumentException("최대 코드 포인트 수는 0 이상이어야 합니다.");
    }
    if (value.codePointCount(0, value.length()) <= maxCodePoints) {
      return value;
    }

    int endIndex = value.offsetByCodePoints(0, maxCodePoints);
    return value.substring(0, endIndex);
  }
}
