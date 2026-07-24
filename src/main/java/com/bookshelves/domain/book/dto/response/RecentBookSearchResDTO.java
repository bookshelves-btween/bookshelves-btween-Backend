package com.bookshelves.domain.book.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record RecentBookSearchResDTO(List<RecentSearchInfo> recentSearches) {

  public record RecentSearchInfo(String keyword, OffsetDateTime searchedAt) {}
}
