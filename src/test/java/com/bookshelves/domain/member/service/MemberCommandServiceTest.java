package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemberCommandServiceTest {

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberCategoryRepository memberCategoryRepository =
      mock(MemberCategoryRepository.class);
  private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
  private final MemberCommandService memberCommandService =
      new MemberCommandService(memberRepository, memberCategoryRepository, categoryRepository);

  @Test
  void updateMyInfoCombinesNicknamePartsAndSavesColor() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of());

    MemberUpdateRequest request =
        MemberUpdateRequest.builder()
            .nicknameNoun("책")
            .nicknameModifier("먹는")
            .nicknameAnimal("여우")
            .profileBackgroundColor(ProfileBackgroundColor.ORANGE)
            .build();

    MemberInfoResponse response = memberCommandService.updateMyInfo(1L, request);

    assertThat(response.getNickname()).isEqualTo("책 먹는 여우");
    assertThat(response.getNicknameNoun()).isEqualTo("책");
    assertThat(response.getNicknameModifier()).isEqualTo("먹는");
    assertThat(response.getNicknameAnimal()).isEqualTo("여우");
    assertThat(response.getProfileBackgroundColor()).isEqualTo(ProfileBackgroundColor.ORANGE);
    verify(memberCategoryRepository, never()).deleteByMember_Id(any());
  }

  @Test
  void updateMyInfoUpdatesColorOnlyWithoutTouchingNicknameOrCategories() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of());

    MemberUpdateRequest request =
        MemberUpdateRequest.builder().profileBackgroundColor(ProfileBackgroundColor.BLUE).build();

    MemberInfoResponse response = memberCommandService.updateMyInfo(1L, request);

    assertThat(response.getProfileBackgroundColor()).isEqualTo(ProfileBackgroundColor.BLUE);
    assertThat(response.getNickname()).isNull();
    verify(memberCategoryRepository, never()).deleteByMember_Id(any());
  }

  @Test
  void updateMyInfoReplacesCategoriesFully() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

    Category selfHelp = mock(Category.class);
    when(selfHelp.getId()).thenReturn(3L);
    when(selfHelp.getName()).thenReturn("자기계발");
    when(categoryRepository.findAllById(List.of(3L))).thenReturn(List.of(selfHelp));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of(selfHelp));

    MemberUpdateRequest request = MemberUpdateRequest.builder().categoryIds(List.of(3L)).build();

    MemberInfoResponse response = memberCommandService.updateMyInfo(1L, request);

    assertThat(response.getCategories()).hasSize(1);
    assertThat(response.getCategories().get(0).id()).isEqualTo(3L);
    verify(memberCategoryRepository).deleteByMember_Id(1L);
    verify(memberCategoryRepository).saveAll(any());
  }

  @Test
  void updateMyInfoDeduplicatesCategoryIdsBeforeValidating() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

    Category selfHelp = mock(Category.class);
    when(selfHelp.getId()).thenReturn(3L);
    when(selfHelp.getName()).thenReturn("자기계발");
    // 같은 ID가 중복으로 와도(distinct 처리 후) 실제 존재하는 카테고리 하나와 개수가 맞아야 함
    when(categoryRepository.findAllById(List.of(3L))).thenReturn(List.of(selfHelp));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of(selfHelp));

    MemberUpdateRequest request =
        MemberUpdateRequest.builder().categoryIds(List.of(3L, 3L)).build();

    MemberInfoResponse response = memberCommandService.updateMyInfo(1L, request);

    assertThat(response.getCategories()).hasSize(1);
  }

  @Test
  void updateMyInfoClearsCategoriesWhenEmptyListProvided() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of());

    MemberUpdateRequest request = MemberUpdateRequest.builder().categoryIds(List.of()).build();

    memberCommandService.updateMyInfo(1L, request);

    verify(memberCategoryRepository).deleteByMember_Id(1L);
    verify(memberCategoryRepository).saveAll(List.of());
  }

  @Test
  void updateMyInfoThrowsNoFieldsToUpdateWhenNothingProvided() {
    MemberUpdateRequest request = MemberUpdateRequest.builder().build();

    assertThatThrownBy(() -> memberCommandService.updateMyInfo(1L, request))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(MemberErrorCode.MEMBER_NO_FIELDS_TO_UPDATE);
    verify(memberRepository, never()).findById(any());
  }

  @Test
  void updateMyInfoThrowsInvalidRequestWhenNicknamePartsPartial() {
    MemberUpdateRequest request = MemberUpdateRequest.builder().nicknameNoun("책").build();

    assertThatThrownBy(() -> memberCommandService.updateMyInfo(1L, request))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(MemberErrorCode.MEMBER_INVALID_REQUEST);
    verify(memberRepository, never()).findById(any());
  }

  @Test
  void updateMyInfoThrowsInvalidRequestWhenCategoryIdDoesNotExist() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(categoryRepository.findAllById(List.of(999L))).thenReturn(List.of());

    MemberUpdateRequest request = MemberUpdateRequest.builder().categoryIds(List.of(999L)).build();

    assertThatThrownBy(() -> memberCommandService.updateMyInfo(1L, request))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(MemberErrorCode.MEMBER_INVALID_REQUEST);
    verify(memberCategoryRepository, never()).deleteByMember_Id(any());
  }

  @Test
  void updateMyInfoThrowsMemberNotFoundWhenMemberDoesNotExist() {
    when(memberRepository.findById(1L)).thenReturn(Optional.empty());

    MemberUpdateRequest request =
        MemberUpdateRequest.builder().profileBackgroundColor(ProfileBackgroundColor.PINK).build();

    assertThatThrownBy(() -> memberCommandService.updateMyInfo(1L, request))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
  }
}
