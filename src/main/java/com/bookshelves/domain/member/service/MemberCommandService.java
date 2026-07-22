package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService {

  private final MemberRepository memberRepository;

  public MemberCommandService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Member createSocialMember(Provider provider, String providerId) {
    return memberRepository.save(Member.createSocialMember(provider, providerId));
  }
}
