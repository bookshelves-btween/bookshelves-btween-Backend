package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.MemberCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {}
