package com.bookshelves.domain.member.repository;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

  Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);
}
