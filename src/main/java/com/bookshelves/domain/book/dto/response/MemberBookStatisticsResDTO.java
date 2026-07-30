package com.bookshelves.domain.book.dto.response;

import java.math.BigDecimal;

public record MemberBookStatisticsResDTO(
    long completedBookCount, long reviewCount, BigDecimal averageRating) {}
