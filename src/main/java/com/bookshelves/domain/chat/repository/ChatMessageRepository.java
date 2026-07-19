package com.bookshelves.domain.chat.repository;

import com.bookshelves.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {}
