package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  List<Category> findAllByOrderByKdcCodeAsc();
}
