package com.bookshelves.domain.chat.dto;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "채팅 메시지")
public record ChatMessagePayload(
    @Schema(description = "메시지 ID", example = "301") Long messageId,
    @Schema(description = "발신 회원 ID", example = "12") Long senderMemberId,
    @Schema(description = "발신 회원 닉네임", example = "책 먹는 여우") String senderNickname,
    @Schema(description = "닉네임 동물", example = "여우") String senderNicknameAnimal,
    @Schema(description = "프로필 배경색", example = "YELLOW")
        ProfileBackgroundColor senderProfileBackgroundColor,
    @Schema(description = "메시지 내용", example = "저는 주인공의 변화가 가장 인상 깊었어요.") String content,
    @Schema(description = "메시지 생성 시각", example = "2026-08-12T19:05:12+09:00")
        OffsetDateTime createdAt) {}
