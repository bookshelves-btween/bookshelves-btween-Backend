package com.bookshelves.domain.member.converter;

import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.member.dto.response.CategoryResponse;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.dto.response.MemberWithdrawResponse;
import com.bookshelves.domain.member.entity.Member;
import java.time.OffsetDateTime;
import java.util.List;

public class MemberConverter {

  private MemberConverter() {}

  public static MemberInfoResponse toMemberInfoResponse(Member member, List<Category> categories) {
    return MemberInfoResponse.builder()
        .id(member.getId())
        .nickname(member.getNickname())
        .nicknameNoun(member.getNicknameNoun())
        .nicknameModifier(member.getNicknameModifier())
        .nicknameAnimal(member.getNicknameAnimal())
        .profileBackgroundColor(member.getProfileBackgroundColor())
        .provider(member.getProvider())
        .memberStatus(member.getStatus())
        .createdAt(member.getCreatedAt())
        .categories(categories.stream().map(MemberConverter::toCategoryResponse).toList())
        .build();
  }

  private static CategoryResponse toCategoryResponse(Category category) {
    return new CategoryResponse(category.getId(), category.getName());
  }

  public static MemberWithdrawResponse toWithdrawResponse(OffsetDateTime scheduledDeletionAt) {
    return MemberWithdrawResponse.builder().scheduledDeletionAt(scheduledDeletionAt).build();
  }
}
