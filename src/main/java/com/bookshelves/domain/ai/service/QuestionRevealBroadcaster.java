package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.converter.AIConverter;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.event.QuestionRevealedEvent;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.chat.dto.ChatFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 질문 커서가 커밋된 뒤 QUESTION 프레임을 전송한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionRevealBroadcaster {

  private final AIQuestionRepository aiQuestionRepository;
  private final SimpMessagingTemplate messagingTemplate;

  // 커밋 후 질문 조회를 위해 새 읽기 트랜잭션을 연다.
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void broadcastRevealedQuestion(QuestionRevealedEvent event) {
    try {
      aiQuestionRepository
          .findByMeetingIdAndQuestionOrder(event.meetingId(), event.questionOrder())
          .ifPresentOrElse(
              question ->
                  messagingTemplate.convertAndSend(
                      ChatFrame.CHATROOM_SUB_DESTINATION + event.chatroomId(),
                      ChatFrame.of(
                          ChatFrame.TYPE_QUESTION,
                          event.chatroomId(),
                          AIConverter.toChatQuestionPayload(question, SeedQuestion.count()))),
              () ->
                  log.error(
                      "공개할 AI 질문이 없다: meetingId={}, order={}",
                      event.meetingId(),
                      event.questionOrder()));
    } catch (Exception e) {
      // 전송 실패 시 클라이언트는 재입장 과정에서 커밋된 최신 질문을 복구한다.
      log.error(
          "QUESTION broadcast 실패: chatroomId={}, order={}",
          event.chatroomId(),
          event.questionOrder(),
          e);
    }
  }
}
