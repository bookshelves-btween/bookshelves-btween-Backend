package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class BookQueryServiceTest {

  @Mock private CategoryRepository categoryRepository;
  @InjectMocks private BookQueryService bookQueryService;

  @Test
  void getCategoriesReturnsCategoriesInRepositoryOrder() {
    Category generalities = category(1L, "000", "총류");
    Category literature = category(9L, "800", "문학");
    given(categoryRepository.findAllByOrderByKdcCodeAsc())
        .willReturn(List.of(generalities, literature));

    CategoryListResDTO result = bookQueryService.getCategories();

    assertThat(result.categories()).hasSize(2);
    assertThat(result.categories().getFirst().id()).isEqualTo(1L);
    assertThat(result.categories().getFirst().kdcCode()).isEqualTo("000");
    assertThat(result.categories().getFirst().name()).isEqualTo("총류");
    assertThat(result.categories().getLast().kdcCode()).isEqualTo("800");
  }

  @Test
  void getCategoriesThrowsBookExceptionWhenDatabaseAccessFails() {
    given(categoryRepository.findAllByOrderByKdcCodeAsc())
        .willThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThatThrownBy(bookQueryService::getCategories)
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.CATEGORY_LIST_FAILED));
  }

  private Category category(Long id, String kdcCode, String name) {
    Category category = mock(Category.class);
    given(category.getId()).willReturn(id);
    given(category.getKdcCode()).willReturn(kdcCode);
    given(category.getName()).willReturn(name);
    return category;
  }
}
