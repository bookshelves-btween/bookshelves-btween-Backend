package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.AIQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIQuestionRepository extends JpaRepository<AIQuestion, Long> {

  // 현재 공개된 질문은 Meeting.currentQuestionOrder로 지정된다 —
  // 질문 5개가 모임 시작 전에 미리 저장되므로 "가장 큰 order = 현재 질문"이 성립하지 않는다.
  Optional<AIQuestion> findByMeetingIdAndQuestionOrder(Long meetingId, Integer questionOrder);

  List<AIQuestion> findAllByMeetingIdOrderByQuestionOrderAsc(Long meetingId);

  long countByMeetingId(Long meetingId);

  void deleteAllByMeetingId(Long meetingId);
}
