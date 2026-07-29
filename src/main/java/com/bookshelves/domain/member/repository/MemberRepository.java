package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

  List<Member> findByStatusAndDeletedAtLessThanEqual(MemberStatus status, LocalDateTime threshold);
}
