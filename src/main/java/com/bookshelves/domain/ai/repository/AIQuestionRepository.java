package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.AIQuestion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIQuestionRepository extends JpaRepository<AIQuestion, Long> {

  Optional<AIQuestion> findTopByMeetingIdOrderByQuestionOrderDesc(Long meetingId);
}
