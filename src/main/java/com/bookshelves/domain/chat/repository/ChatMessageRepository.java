package com.bookshelves.domain.chat.repository;

import com.bookshelves.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  // 채팅 렌더링 순서대로 발신자와 함께 조회한다.
  @Query(
      "select cm from ChatMessage cm join fetch cm.senderMember where cm.chatRoom.id = :chatroomId order by cm.id asc")
  List<ChatMessage> findAllWithSenderByChatroomId(@Param("chatroomId") Long chatroomId);
}
