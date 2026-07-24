package com.bookshelves.domain.book.config;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CategoryDataInitializer implements ApplicationRunner {

  private static final List<CategoryMasterData> MASTER_CATEGORIES =
      List.of(
          new CategoryMasterData("000", "총류"),
          new CategoryMasterData("100", "철학"),
          new CategoryMasterData("200", "종교"),
          new CategoryMasterData("300", "사회과학"),
          new CategoryMasterData("400", "자연과학"),
          new CategoryMasterData("500", "기술과학"),
          new CategoryMasterData("600", "예술"),
          new CategoryMasterData("700", "언어"),
          new CategoryMasterData("800", "문학"),
          new CategoryMasterData("900", "역사"));

  private final CategoryRepository categoryRepository;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    List<Category> savedCategories = categoryRepository.findAll();
    Map<String, Category> categoryByKdcCode =
        savedCategories.stream()
            .filter(category -> category.getKdcCode() != null)
            .collect(Collectors.toMap(Category::getKdcCode, Function.identity()));
    Map<String, Category> categoryByName =
        savedCategories.stream()
            .collect(
                Collectors.toMap(
                    Category::getName, Function.identity(), (existing, duplicate) -> existing));

    List<Category> newCategories = new ArrayList<>();
    for (CategoryMasterData masterData : MASTER_CATEGORIES) {
      Category category = categoryByKdcCode.get(masterData.kdcCode());
      if (category != null) {
        category.updateMasterData(masterData.kdcCode(), masterData.name());
        continue;
      }

      Category legacyCategory = categoryByName.get(masterData.name());
      if (legacyCategory != null) {
        legacyCategory.updateMasterData(masterData.kdcCode(), masterData.name());
        continue;
      }

      newCategories.add(Category.create(masterData.kdcCode(), masterData.name()));
    }

    categoryRepository.saveAll(newCategories);
  }

  private record CategoryMasterData(String kdcCode, String name) {}
}
