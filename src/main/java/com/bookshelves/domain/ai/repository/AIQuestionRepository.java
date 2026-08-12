package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.AIQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIQuestionRepository extends JpaRepository<AIQuestion, Long> {

  // 현재 질문은 미리 저장된 질문 중 Meeting.currentQuestionOrder로 조회한다.
  Optional<AIQuestion> findByMeetingIdAndQuestionOrder(Long meetingId, Integer questionOrder);

  List<AIQuestion> findAllByMeetingIdOrderByQuestionOrderAsc(Long meetingId);

  long countByMeetingId(Long meetingId);

  void deleteAllByMeetingId(Long meetingId);
}
