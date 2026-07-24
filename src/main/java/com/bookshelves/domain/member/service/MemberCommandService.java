package com.bookshelves.domain.member.service;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.member.converter.MemberConverter;
import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.dto.response.MemberWithdrawResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.entity.MemberCategory;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.RedisTokenRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberCommandService {

  // Member 엔티티 컬럼 길이(nickname_noun/modifier/animal=30, nickname=50)와 반드시 일치해야 한다.
  private static final int NICKNAME_PART_MAX_LENGTH = 30;
  private static final int NICKNAME_MAX_LENGTH = 50;

  private final MemberRepository memberRepository;
  private final MemberCategoryRepository memberCategoryRepository;
  private final CategoryRepository categoryRepository;
  private final RedisTokenRepository redisTokenRepository;

  public MemberCommandService(
      MemberRepository memberRepository,
      MemberCategoryRepository memberCategoryRepository,
      CategoryRepository categoryRepository,
      RedisTokenRepository redisTokenRepository) {
    this.memberRepository = memberRepository;
    this.memberCategoryRepository = memberCategoryRepository;
    this.categoryRepository = categoryRepository;
    this.redisTokenRepository = redisTokenRepository;
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

  public MemberWithdrawResponse withdraw(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ProjectException(MemberErrorCode.MEMBER_NOT_FOUND));

    member.withdraw();
    redisTokenRepository.deleteAllTokens(memberId);

    OffsetDateTime scheduledDeletionAt =
        member
            .getDeletedAt()
            .plusDays(Member.RESTORE_PERIOD_DAYS)
            .atZone(Member.SERVICE_ZONE)
            .toOffsetDateTime();

    return MemberConverter.toWithdrawResponse(scheduledDeletionAt);
  }

  private void updateCategories(Long memberId, Member member, List<Long> categoryIds) {
    // List.of(...) 등 일부 불변 리스트 구현은 contains(null) 자체가 NPE를 던지므로 스트림으로 검사한다.
    if (categoryIds.stream().anyMatch(Objects::isNull)) {
      throw new ProjectException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

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

    if (providedCount == 3) {
      validateNicknameLength(request);
    }
  }

  private void validateNicknameLength(MemberUpdateRequest request) {
    String noun = request.getNicknameNoun();
    String modifier = request.getNicknameModifier();
    String animal = request.getNicknameAnimal();

    boolean anyPartTooLong =
        noun.length() > NICKNAME_PART_MAX_LENGTH
            || modifier.length() > NICKNAME_PART_MAX_LENGTH
            || animal.length() > NICKNAME_PART_MAX_LENGTH;

    int combinedLength = noun.length() + 1 + modifier.length() + 1 + animal.length();

    if (anyPartTooLong || combinedLength > NICKNAME_MAX_LENGTH) {
      throw new ProjectException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }
  }

  private boolean hasAllNicknameParts(MemberUpdateRequest request) {
    return request.getNicknameNoun() != null
        && request.getNicknameModifier() != null
        && request.getNicknameAnimal() != null;
  }
}
