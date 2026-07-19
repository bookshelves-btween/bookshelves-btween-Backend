package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.MemberTerms;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberTermsRepository extends JpaRepository<MemberTerms, Long> {}
