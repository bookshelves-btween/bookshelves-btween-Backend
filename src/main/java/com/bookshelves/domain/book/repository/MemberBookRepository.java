package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {}
