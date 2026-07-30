package com.bookshelves.domain.ai.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryAxisTest {

  @Test
  void ordersAxesByDisplayOrder() {
    List<SummaryAxis> ordered = SummaryAxis.ordered();

    assertThat(ordered)
        .containsExactly(SummaryAxis.KEY_ARGUMENT, SummaryAxis.REACTION, SummaryAxis.LIFE_LINK);
    assertThat(ordered.stream().map(SummaryAxis::getDisplayOrder)).containsExactly(1, 2, 3);
  }

  @Test
  void everyAxisCarriesDescriptionForPrompt() {
    assertThat(SummaryAxis.values())
        .allSatisfy(axis -> assertThat(axis.getDescription()).isNotBlank());
  }
}
