package com.bookshelves.domain.book.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class IsbnNormalizer {

  private IsbnNormalizer() {}

  public static Optional<String> normalize(String rawIsbn) {
    if (rawIsbn == null || rawIsbn.isBlank()) {
      return Optional.empty();
    }

    String[] candidates =
        Arrays.stream(rawIsbn.trim().split("\\s+"))
            .map(candidate -> candidate.replace("-", "").toUpperCase(Locale.ROOT))
            .toArray(String[]::new);

    Optional<String> isbn13 =
        Arrays.stream(candidates).filter(IsbnNormalizer::isValidIsbn13).findFirst();
    if (isbn13.isPresent()) {
      return isbn13;
    }

    return Arrays.stream(candidates).filter(IsbnNormalizer::isValidIsbn10).findFirst();
  }

  public static String toIsbn13(String isbn) {
    if (!isValidIsbn10(isbn)) {
      return isbn;
    }

    String isbnWithoutCheckDigit = "978" + isbn.substring(0, 9);
    int sum = 0;
    for (int index = 0; index < isbnWithoutCheckDigit.length(); index++) {
      int digit = isbnWithoutCheckDigit.charAt(index) - '0';
      sum += digit * (index % 2 == 0 ? 1 : 3);
    }
    int checkDigit = (10 - sum % 10) % 10;
    return isbnWithoutCheckDigit + checkDigit;
  }

  private static boolean isValidIsbn13(String isbn) {
    if (!isbn.matches("\\d{13}")) {
      return false;
    }

    int sum = 0;
    for (int index = 0; index < isbn.length(); index++) {
      int digit = isbn.charAt(index) - '0';
      sum += digit * (index % 2 == 0 ? 1 : 3);
    }
    return sum % 10 == 0;
  }

  private static boolean isValidIsbn10(String isbn) {
    if (!isbn.matches("\\d{9}[\\dX]")) {
      return false;
    }

    int sum = 0;
    for (int index = 0; index < isbn.length(); index++) {
      char character = isbn.charAt(index);
      int digit = character == 'X' ? 10 : character - '0';
      sum += digit * (10 - index);
    }
    return sum % 11 == 0;
  }
}
