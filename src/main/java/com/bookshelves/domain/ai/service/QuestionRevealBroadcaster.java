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

// 커서 증가가 커밋된 뒤에만 QUESTION 프레임을 내보낸다.
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionRevealBroadcaster {

  private final AIQuestionRepository aiQuestionRepository;
  private final SimpMessagingTemplate messagingTemplate;

  // AFTER_COMMIT 리스너는 트랜잭션이 끝난 뒤에 실행되므로 조회를 위해 새 트랜잭션을 연다
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
      // 커서는 이미 커밋됐다 — 프레임 유실 시 클라이언트는 재입장(입장 API)으로 최신 질문을 복구한다.
      // MVP에서는 outbox/재전송 없이 이 복구 경로를 계약으로 둔다.
      log.error(
          "QUESTION broadcast 실패: chatroomId={}, order={}",
          event.chatroomId(),
          event.questionOrder(),
          e);
    }
  }
}
