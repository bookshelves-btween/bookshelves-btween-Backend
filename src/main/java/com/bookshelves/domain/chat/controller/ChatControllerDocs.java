package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "채팅", description = "모임 채팅 API")
public interface ChatControllerDocs {

  @Operation(
      summary = "채팅방 입장",
      description =
          "채팅방 입장 시 호출. 화면 상단 메타(책 제목·인원·시간), 현재 AI 질문·투표 현황, "
              + "전체 메시지(오래된 → 최신)를 한 번에 반환한다. 재연결 시에도 다시 호출해 messageId로 중복 제거. "
              + "실시간 변화는 SUB /sub/chatrooms/{chatroomId} 구독 프레임으로 갱신한다.")
  @GetMapping("/api/v1/chatrooms/{chatroomId}")
  ResponseEntity<ApiResponse<ChatRoomEnterResponse>> enterChatRoom(@PathVariable Long chatroomId);
}
