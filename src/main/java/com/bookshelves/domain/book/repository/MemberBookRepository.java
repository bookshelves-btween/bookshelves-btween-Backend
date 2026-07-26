package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.entity.MemberBook;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberBookRepository extends JpaRepository<MemberBook, Long> {

  Optional<MemberBook> findByMemberIdAndBookId(Long memberId, Long bookId);
}
