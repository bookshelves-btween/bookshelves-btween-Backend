package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO.CategoryInfo;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookQueryService {

  private final CategoryRepository categoryRepository;

  public CategoryListResDTO getCategories() {
    try {
      List<CategoryInfo> categories =
          categoryRepository.findAllByOrderByKdcCodeAsc().stream()
              .map(this::toCategoryInfo)
              .toList();
      return new CategoryListResDTO(categories);
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.CATEGORY_LIST_FAILED);
    }
  }

  private CategoryInfo toCategoryInfo(Category category) {
    return new CategoryInfo(category.getId(), category.getKdcCode(), category.getName());
  }
}
