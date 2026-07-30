package com.bookshelves.domain.book.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MemberBookStatisticsResDTO(
    int year,
    int month,
    long completedBookCount,
    long reviewCount,
    BigDecimal averageRating,
    List<CategoryStatistic> categoryStatistics) {

  public record CategoryStatistic(String name, long count, int percentage) {}
}
