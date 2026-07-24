package com.bookshelves.domain.chat.repository;

import com.bookshelves.domain.chat.entity.ChatRoom;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  @Query(
      "select cr from ChatRoom cr join fetch cr.meeting m join fetch m.book where cr.id = :chatroomId")
  Optional<ChatRoom> findByIdWithMeetingAndBook(@Param("chatroomId") Long chatroomId);

  Optional<ChatRoom> findByMeetingId(Long meetingId);

  List<ChatRoom> findAllByMeetingIdIn(Collection<Long> meetingIds);
}
