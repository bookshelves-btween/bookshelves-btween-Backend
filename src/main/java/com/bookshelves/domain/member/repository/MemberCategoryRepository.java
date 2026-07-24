package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.member.entity.MemberCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {

  @Query("SELECT mc.category FROM MemberCategory mc WHERE mc.member.id = :memberId")
  List<Category> findCategoriesByMemberId(@Param("memberId") Long memberId);

  void deleteByMember_Id(Long memberId);
}
