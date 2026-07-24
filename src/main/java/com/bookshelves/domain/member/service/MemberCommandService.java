package com.bookshelves.domain.member.service;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.member.converter.MemberConverter;
import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.entity.MemberCategory;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService {

  private final MemberRepository memberRepository;
  private final MemberCategoryRepository memberCategoryRepository;
  private final CategoryRepository categoryRepository;

  public MemberCommandService(
      MemberRepository memberRepository,
      MemberCategoryRepository memberCategoryRepository,
      CategoryRepository categoryRepository) {
    this.memberRepository = memberRepository;
    this.memberCategoryRepository = memberCategoryRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Member createSocialMember(Provider provider, String providerId) {
    return memberRepository.save(Member.createSocialMember(provider, providerId));
  }

  public MemberInfoResponse updateMyInfo(Long memberId, MemberUpdateRequest request) {
    validateHasAtLeastOneField(request);
    validateNicknamePartsAllOrNone(request);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ProjectException(MemberErrorCode.MEMBER_NOT_FOUND));

    if (hasAllNicknameParts(request)) {
      member.updateNickname(
          request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
    }

    if (request.getProfileBackgroundColor() != null) {
      member.updateProfileBackgroundColor(request.getProfileBackgroundColor());
    }

    if (request.getCategoryIds() != null) {
      updateCategories(memberId, member, request.getCategoryIds());
    }

    List<Category> categories = memberCategoryRepository.findCategoriesByMemberId(memberId);
    return MemberConverter.toMemberInfoResponse(member, categories);
  }

  private void updateCategories(Long memberId, Member member, List<Long> categoryIds) {
    List<Long> distinctCategoryIds = categoryIds.stream().distinct().toList();
    List<Category> categories = categoryRepository.findAllById(distinctCategoryIds);
    if (categories.size() != distinctCategoryIds.size()) {
      throw new ProjectException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

    memberCategoryRepository.deleteByMember_Id(memberId);
    memberCategoryRepository.saveAll(
        categories.stream().map(category -> MemberCategory.create(member, category)).toList());
  }

  private void validateHasAtLeastOneField(MemberUpdateRequest request) {
    boolean noFields =
        request.getNicknameNoun() == null
            && request.getNicknameModifier() == null
            && request.getNicknameAnimal() == null
            && request.getProfileBackgroundColor() == null
            && request.getCategoryIds() == null;

    if (noFields) {
      throw new ProjectException(MemberErrorCode.MEMBER_NO_FIELDS_TO_UPDATE);
    }
  }

  private void validateNicknamePartsAllOrNone(MemberUpdateRequest request) {
    long providedCount =
        Stream.of(
                request.getNicknameNoun(),
                request.getNicknameModifier(),
                request.getNicknameAnimal())
            .filter(Objects::nonNull)
            .count();

    if (providedCount != 0 && providedCount != 3) {
      throw new ProjectException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }
  }

  private boolean hasAllNicknameParts(MemberUpdateRequest request) {
    return request.getNicknameNoun() != null
        && request.getNicknameModifier() != null
        && request.getNicknameAnimal() != null;
  }
}
