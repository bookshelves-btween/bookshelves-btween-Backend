package com.bookshelves.domain.chat.repository;

import com.bookshelves.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // 오래된 → 최신 순 (채팅 렌더 순서). 최대 60분·6명 규모라 페이지네이션 없이 전체 조회
  @Query(
      "select cm from ChatMessage cm join fetch cm.senderMember where cm.chatRoom.id = :chatroomId order by cm.id asc")
  List<ChatMessage> findAllWithSenderByChatroomId(@Param("chatroomId") Long chatroomId);
}
