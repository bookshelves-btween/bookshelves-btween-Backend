package com.bookshelves.domain.book.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IsbnNormalizerTest {

  @Test
  void normalizePrefersIsbn13WhenBothFormatsExist() {
    assertThat(IsbnNormalizer.normalize("8996991341 9788996991342")).contains("9788996991342");
  }

  @Test
  void normalizeUsesIsbn10WhenIsbn13DoesNotExist() {
    assertThat(IsbnNormalizer.normalize("8996991341")).contains("8996991341");
  }

  @Test
  void normalizeRemovesHyphensAndAcceptsLowercaseX() {
    assertThat(IsbnNormalizer.normalize("0-8044-2957-x")).contains("080442957X");
  }

  @Test
  void normalizeReturnsEmptyForBlankOrInvalidIsbn() {
    assertThat(IsbnNormalizer.normalize("")).isEmpty();
    assertThat(IsbnNormalizer.normalize("1234567890 9781234567890")).isEmpty();
  }
}
