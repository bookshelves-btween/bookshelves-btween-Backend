package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select member from Member member where member.id = :id")
  Optional<Member> findByIdForUpdate(@Param("id") Long id);

  Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);

  @Query("SELECT m.status FROM Member m WHERE m.id = :id")
  Optional<MemberStatus> findStatusById(@Param("id") Long id);

  // 익명화 배치 대상 조회 전용. 스케줄러가 id만 사용하므로 전체 엔티티 대신 id만 프로젝션하고,
  // Pageable로 한 번에 처리하는 건수를 제한해 대량 탈퇴가 몰린 날에도 메모리에 한 번에 올리지
  // 않는다.
  @Query("SELECT m.id FROM Member m WHERE m.status = :status AND m.deletedAt <= :threshold")
  List<Long> findIdsByStatusAndDeletedAtLessThanEqual(
      @Param("status") MemberStatus status,
      @Param("threshold") LocalDateTime threshold,
      Pageable pageable);
}
