package com.bookshelves.domain.book.dto.response;

public record MemberBookUpsertResDTO(MemberBookHistoryInfo memberBookHistory) {

  public record MemberBookHistoryInfo(Long id) {}

  public static MemberBookUpsertResDTO withoutHistory() {
    return new MemberBookUpsertResDTO(null);
  }

  public static MemberBookUpsertResDTO withHistory(Long historyId) {
    return new MemberBookUpsertResDTO(new MemberBookHistoryInfo(historyId));
  }
}
