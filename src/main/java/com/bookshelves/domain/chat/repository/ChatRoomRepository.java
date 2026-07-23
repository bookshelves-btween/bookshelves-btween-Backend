package com.bookshelves.domain.chat.repository;

import com.bookshelves.domain.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  Optional<ChatRoom> findByMeetingId(Long meetingId);
}
