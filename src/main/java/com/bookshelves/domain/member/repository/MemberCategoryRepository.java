package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.member.entity.MemberCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCategoryRepository extends JpaRepository<MemberCategory, Long> {

  @Query("SELECT mc.category FROM MemberCategory mc WHERE mc.member.id = :memberId")
  List<Category> findCategoriesByMemberId(@Param("memberId") Long memberId);

  // 벌크 delete로 즉시 실행되어야 한다. derived delete(로드 후 개별 remove)로 두면
  // Hibernate가 flush 시 INSERT를 DELETE보다 먼저 내보내 재선택된 카테고리에서
  // uk_member_category_member_category 유니크 충돌이 날 수 있다.
  @Modifying
  @Query("DELETE FROM MemberCategory mc WHERE mc.member.id = :memberId")
  void deleteByMember_Id(@Param("memberId") Long memberId);
}
