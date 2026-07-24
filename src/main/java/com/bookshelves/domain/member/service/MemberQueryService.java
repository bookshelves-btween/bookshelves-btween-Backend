package com.bookshelves.domain.member.service;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.member.converter.MemberConverter;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberQueryService {

  private final MemberRepository memberRepository;
  private final MemberCategoryRepository memberCategoryRepository;

  public MemberQueryService(
      MemberRepository memberRepository, MemberCategoryRepository memberCategoryRepository) {
    this.memberRepository = memberRepository;
    this.memberCategoryRepository = memberCategoryRepository;
  }

  public MemberInfoResponse getMyInfo(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ProjectException(MemberErrorCode.MEMBER_NOT_FOUND));

    List<Category> categories = memberCategoryRepository.findCategoriesByMemberId(memberId);

    return MemberConverter.toMemberInfoResponse(member, categories);
  }
}
