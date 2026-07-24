package com.bookshelves.domain.book.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryDataInitializerTest {

  @Mock private CategoryRepository categoryRepository;
  @InjectMocks private CategoryDataInitializer categoryDataInitializer;

  @Test
  void runCreatesTenMasterCategoriesWhenDatabaseIsEmpty() {
    given(categoryRepository.findAll()).willReturn(List.of());
    ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);

    categoryDataInitializer.run(null);

    verify(categoryRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(10);
    assertThat(captor.getValue().getFirst().getKdcCode()).isEqualTo("000");
    assertThat(captor.getValue().getFirst().getName()).isEqualTo("총류");
    assertThat(captor.getValue().getLast().getKdcCode()).isEqualTo("900");
    assertThat(captor.getValue().getLast().getName()).isEqualTo("역사");
  }

  @Test
  void runDoesNotCreateDuplicatesWhenMasterCategoriesAlreadyExist() {
    List<Category> categories =
        List.of(
            Category.create("000", "총류"),
            Category.create("100", "철학"),
            Category.create("200", "종교"),
            Category.create("300", "사회과학"),
            Category.create("400", "자연과학"),
            Category.create("500", "기술과학"),
            Category.create("600", "예술"),
            Category.create("700", "언어"),
            Category.create("800", "문학"),
            Category.create("900", "역사"));
    given(categoryRepository.findAll()).willReturn(categories);

    categoryDataInitializer.run(null);

    verify(categoryRepository).saveAll(List.of());
  }

  @Test
  void runBackfillsKdcCodeOnLegacyCategoryMatchedByName() {
    Category legacyCategory = Category.create(null, "문학");
    given(categoryRepository.findAll()).willReturn(List.of(legacyCategory));

    categoryDataInitializer.run(null);

    assertThat(legacyCategory.getKdcCode()).isEqualTo("800");
  }
}
