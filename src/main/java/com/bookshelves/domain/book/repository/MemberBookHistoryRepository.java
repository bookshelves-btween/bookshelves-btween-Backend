package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBookHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBookHistoryRepository extends JpaRepository<MemberBookHistory, Long> {}
