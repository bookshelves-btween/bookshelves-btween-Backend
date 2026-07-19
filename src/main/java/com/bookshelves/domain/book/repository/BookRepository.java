package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {}
