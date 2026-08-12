package com.bookshelves.domain.member.service;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.member.converter.MemberConverter;
import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.request.OnboardingRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.dto.response.MemberWithdrawResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.entity.MemberCategory;
import com.bookshelves.domain.member.entity.MemberTerms;
import com.bookshelves.domain.member.entity.Terms;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.exception.MemberException;
import com.bookshelves.domain.member.exception.TermsErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.member.repository.MemberTermsRepository;
import com.bookshelves.domain.member.repository.TermsRepository;
import com.bookshelves.global.security.RedisTokenRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  private final TermsRepository termsRepository;
  private final MemberTermsRepository memberTermsRepository;
  private final RedisTokenRepository redisTokenRepository;

  public MemberCommandService(
      MemberRepository memberRepository,
      MemberCategoryRepository memberCategoryRepository,
      CategoryRepository categoryRepository,
      TermsRepository termsRepository,
      MemberTermsRepository memberTermsRepository,
      RedisTokenRepository redisTokenRepository) {
    this.memberRepository = memberRepository;
    this.memberCategoryRepository = memberCategoryRepository;
    this.categoryRepository = categoryRepository;
    this.termsRepository = termsRepository;
    this.memberTermsRepository = memberTermsRepository;
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
            .findByIdForUpdate(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

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

  public MemberInfoResponse completeOnboarding(Long memberId, OnboardingRequest request) {
    Member member =
        memberRepository
            .findByIdForUpdate(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

    if (member.getStatus() != MemberStatus.PENDING_ONBOARDING) {
      throw new MemberException(MemberErrorCode.MEMBER_ALREADY_ONBOARDED);
    }

    validateNicknameLength(
        request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
    validateNicknameWords(
        request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
    validateAnimalColorMapping(request.getNicknameAnimal(), request.getProfileBackgroundColor());
    validateRequiredTermsAgreed(request.getAgreedTermsIds());

    member.updateNickname(
        request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
    member.updateProfileBackgroundColor(request.getProfileBackgroundColor());

    if (request.getCategoryIds() != null) {
      updateCategories(memberId, member, request.getCategoryIds());
    }

    saveAgreedTerms(member, request.getAgreedTermsIds());

    member.completeOnboarding();

    List<Category> categories = memberCategoryRepository.findCategoriesByMemberId(memberId);
    return MemberConverter.toMemberInfoResponse(member, categories);
  }

  public void anonymizeMember(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 스케줄러의 대상 조회 이후 복구되었거나 상태가 바뀌었을 수 있으므로 여기서 다시 검증한다.
    LocalDateTime threshold =
        LocalDateTime.now(Member.SERVICE_ZONE).minusDays(Member.RESTORE_PERIOD_DAYS);
    if (member.getStatus() != MemberStatus.WITHDRAWN
        || member.getDeletedAt() == null
        || member.getDeletedAt().isAfter(threshold)) {
      return;
    }

    member.anonymize();
    // 관심 장르는 개인정보처리방침상 탈퇴 완료 시 파기 대상인 개인정보다. category_id가
    // NOT NULL이라 값을 비울 수 없어, 이 데이터의 익명화는 곧 row 삭제를 의미한다.
    memberCategoryRepository.deleteByMember_Id(memberId);
  }

  public MemberWithdrawResponse withdraw(Long memberId) {
    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

    if (member.getStatus() != MemberStatus.ACTIVE) {
      throw new MemberException(MemberErrorCode.MEMBER_NOT_ACTIVE);
    }

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

  private void validateRequiredTermsAgreed(List<Long> agreedTermsIds) {
    List<Long> agreed = agreedTermsIds == null ? List.of() : agreedTermsIds;
    List<Long> requiredTermsIds =
        termsRepository.findByIsActiveTrueAndIsRequiredTrue().stream().map(Terms::getId).toList();

    if (!agreed.containsAll(requiredTermsIds)) {
      throw new MemberException(TermsErrorCode.TERMS_REQUIRED_NOT_AGREED);
    }
  }

  private void saveAgreedTerms(Member member, List<Long> agreedTermsIds) {
    if (agreedTermsIds == null || agreedTermsIds.isEmpty()) {
      return;
    }

    // List.of(...) 등 일부 불변 리스트 구현은 contains(null) 자체가 NPE를 던지므로 스트림으로 검사한다.
    if (agreedTermsIds.stream().anyMatch(Objects::isNull)) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

    Set<Long> distinctTermsIds = Set.copyOf(agreedTermsIds);
    List<Terms> terms = termsRepository.findAllById(distinctTermsIds);
    if (terms.size() != distinctTermsIds.size()) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

    memberTermsRepository.saveAll(terms.stream().map(t -> MemberTerms.create(member, t)).toList());
  }

  private void updateCategories(Long memberId, Member member, List<Long> categoryIds) {
    // List.of(...) 등 일부 불변 리스트 구현은 contains(null) 자체가 NPE를 던지므로 스트림으로 검사한다.
    if (categoryIds.stream().anyMatch(Objects::isNull)) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

    List<Long> distinctCategoryIds = categoryIds.stream().distinct().toList();
    List<Category> categories = categoryRepository.findAllById(distinctCategoryIds);
    if (categories.size() != distinctCategoryIds.size()) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
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
      throw new MemberException(MemberErrorCode.MEMBER_NO_FIELDS_TO_UPDATE);
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
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }

    if (providedCount == 3) {
      validateNicknameLength(
          request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
      validateNicknameWords(
          request.getNicknameNoun(), request.getNicknameModifier(), request.getNicknameAnimal());
    }
  }

  private void validateNicknameLength(String noun, String modifier, String animal) {
    boolean anyPartTooLong =
        noun.length() > NICKNAME_PART_MAX_LENGTH
            || modifier.length() > NICKNAME_PART_MAX_LENGTH
            || animal.length() > NICKNAME_PART_MAX_LENGTH;

    int combinedLength = noun.length() + 1 + modifier.length() + 1 + animal.length();

    if (anyPartTooLong || combinedLength > NICKNAME_MAX_LENGTH) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }
  }

  private void validateNicknameWords(String noun, String modifier, String animal) {
    boolean allowed =
        NicknameWords.NOUNS.contains(noun)
            && NicknameWords.MODIFIERS.contains(modifier)
            && NicknameWords.ANIMALS.contains(animal);

    if (!allowed) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }
  }

  private void validateAnimalColorMapping(String animal, ProfileBackgroundColor color) {
    if (NicknameAnimalColors.DEFAULT_COLORS.get(animal) != color) {
      throw new MemberException(MemberErrorCode.MEMBER_INVALID_REQUEST);
    }
  }

  private boolean hasAllNicknameParts(MemberUpdateRequest request) {
    return request.getNicknameNoun() != null
        && request.getNicknameModifier() != null
        && request.getNicknameAnimal() != null;
  }
}
