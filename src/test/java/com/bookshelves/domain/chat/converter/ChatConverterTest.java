package com.bookshelves.domain.chat.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.chat.dto.ChatMessagePayload;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import org.junit.jupiter.api.Test;

class ChatConverterTest {

  @Test
  void chatMessagePayloadContainsSenderProfile() {
    // 닉네임 세 부분은 NicknameWords의 고정 목록에서만 나온다. 목록에 없는 값으로 픽스처를 만들면
    // 컨버터는 통과하지만 실제로는 존재할 수 없는 회원이 된다.
    Member sender = Member.createSocialMember(Provider.KAKAO, "provider-id");
    sender.updateNickname("책", "먹는", "곰");
    sender.updateProfileBackgroundColor(ProfileBackgroundColor.BROWN);
    ChatMessage message = ChatMessage.builder().senderMember(sender).message("안녕하세요").build();

    ChatMessagePayload payload = ChatConverter.toChatMessagePayload(message);

    assertThat(payload.senderNickname()).isEqualTo("책 먹는 곰");
    assertThat(payload.senderNicknameAnimal()).isEqualTo("곰");
    assertThat(payload.senderProfileBackgroundColor()).isEqualTo(ProfileBackgroundColor.BROWN);
  }
}
