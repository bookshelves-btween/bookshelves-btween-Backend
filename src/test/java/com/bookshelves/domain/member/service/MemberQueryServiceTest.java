package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.exception.MemberErrorCode;
import com.bookshelves.domain.member.repository.MemberCategoryRepository;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemberQueryServiceTest {

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberCategoryRepository memberCategoryRepository =
      mock(MemberCategoryRepository.class);
  private final MemberQueryService memberQueryService =
      new MemberQueryService(memberRepository, memberCategoryRepository);

  @Test
  void getMyInfoReturnsMemberWithSelectedCategories() {
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(1L);
    when(member.getNickname()).thenReturn("행복한 사자");
    when(member.getNicknameNoun()).thenReturn("사자");
    when(member.getNicknameModifier()).thenReturn("행복한");
    when(member.getNicknameAnimal()).thenReturn("사자");
    when(member.getProfileBackgroundColor()).thenReturn(ProfileBackgroundColor.ORANGE);
    when(member.getProvider()).thenReturn(Provider.KAKAO);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(member.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 7, 1, 10, 0));
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

    Category novel = mock(Category.class);
    when(novel.getId()).thenReturn(1L);
    when(novel.getName()).thenReturn("소설");
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of(novel));

    MemberInfoResponse response = memberQueryService.getMyInfo(1L);

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getNickname()).isEqualTo("행복한 사자");
    assertThat(response.getProfileBackgroundColor()).isEqualTo(ProfileBackgroundColor.ORANGE);
    assertThat(response.getProvider()).isEqualTo(Provider.KAKAO);
    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
    assertThat(response.getCategories()).hasSize(1);
    assertThat(response.getCategories().get(0).id()).isEqualTo(1L);
    assertThat(response.getCategories().get(0).name()).isEqualTo("소설");
  }

  @Test
  void getMyInfoReturnsEmptyCategoriesWhenNoneSelected() {
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(1L);
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(memberCategoryRepository.findCategoriesByMemberId(1L)).thenReturn(List.of());

    MemberInfoResponse response = memberQueryService.getMyInfo(1L);

    assertThat(response.getCategories()).isEmpty();
  }

  @Test
  void getMyInfoThrowsMemberNotFoundWhenMemberDoesNotExist() {
    when(memberRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> memberQueryService.getMyInfo(1L))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
  }
}
