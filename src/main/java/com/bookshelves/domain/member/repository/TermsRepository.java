package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.Terms;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsRepository extends JpaRepository<Terms, Long> {

  List<Terms> findByIsRequiredTrue();
}
