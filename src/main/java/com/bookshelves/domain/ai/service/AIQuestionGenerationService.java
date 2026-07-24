package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.client.GeminiQuestionClient;
import com.bookshelves.domain.ai.converter.AIConverter;
import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// 정족수 도달 시 다음 AI 질문을 생성한다. LLM 호출은 별도 스레드에서 수행하고,
// 저장 커밋이 끝난 뒤에만 투표 라운드를 리셋하고 QUESTION 프레임을 broadcast한다.
@Slf4j
@Service
public class AIQuestionGenerationService {

  // 모임당 AI 질문 최대 개수 — 명세 maxQuestions
  public static final int MAX_QUESTIONS = 5;

  // LLM 실패 시에도 QUESTION 프레임은 나가야 하므로 순서별 기본 질문으로 폴백한다
  private static final List<String> FALLBACK_QUESTIONS =
      List.of(
          "이 책에서 가장 인상 깊었던 장면은 무엇이었나요?",
          "책을 읽으며 공감하기 어려웠던 부분이 있었나요?",
          "이 책의 주인공과 같은 상황이라면 어떤 선택을 했을 것 같나요?",
          "이 책이 지금의 나에게 남긴 질문은 무엇인가요?",
          "이 책을 한 문장으로 소개한다면 뭐라고 말하고 싶나요?");

  private final ChatRoomRepository chatRoomRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final QuestionVoteStore questionVoteStore;
  private final GeminiQuestionClient geminiQuestionClient;
  private final SimpMessagingTemplate messagingTemplate;
  private final TaskExecutor taskExecutor;
  private final TransactionTemplate transactionTemplate;

  public AIQuestionGenerationService(
      ChatRoomRepository chatRoomRepository,
      AIQuestionRepository aiQuestionRepository,
      QuestionVoteStore questionVoteStore,
      GeminiQuestionClient geminiQuestionClient,
      SimpMessagingTemplate messagingTemplate,
      // WebSocket 브로커 설정이 채널용 TaskExecutor 빈을 여럿 등록하므로 전용 빈을 명시 지정한다
      @Qualifier("aiQuestionTaskExecutor") TaskExecutor taskExecutor,
      TransactionTemplate transactionTemplate) {
    this.chatRoomRepository = chatRoomRepository;
    this.aiQuestionRepository = aiQuestionRepository;
    this.questionVoteStore = questionVoteStore;
    this.geminiQuestionClient = geminiQuestionClient;
    this.messagingTemplate = messagingTemplate;
    this.taskExecutor = taskExecutor;
    this.transactionTemplate = transactionTemplate;
  }

  // 생성권 선점(tryBeginGeneration)은 비동기 작업이 실행될 때가 아니라 "제출 시점"에 해야 한다 —
  // 실행 시점에 선점하면 정족수 도달 후 큐에 쌓인 트리거들이 앞 작업 종료 후 차례로 선점에 성공해
  // 한 라운드에서 질문이 연달아 생성된다. 선점과 동시에 라운드가 닫혀 새 투표도 거부된다.
  public void requestGeneration(Long chatroomId) {
    if (!questionVoteStore.tryBeginGeneration(chatroomId)) {
      return;
    }

    boolean submitted = false;
    try {
      taskExecutor.execute(
          () -> {
            boolean generated = false;
            try {
              generated = generate(chatroomId);
            } catch (Exception e) {
              log.error("AI 질문 생성 실패: chatroomId={}", chatroomId, e);
            } finally {
              if (!generated) {
                // 실패한 라운드의 표를 정리한다 — 남겨두면 전원이 이미 투표한 방은
                // 이후 요청이 전부 중복 투표로 거부되어 라운드가 영구 정체된다.
                // 표를 비우면 참여자들이 재투표로 생성을 다시 시도할 수 있다.
                // (generating 플래그가 아직 켜져 있어 정리 전 새 표가 끼어들지 못한다)
                questionVoteStore.clearVotes(chatroomId);
              }
              questionVoteStore.endGeneration(chatroomId);
            }
          });
      submitted = true;
    } finally {
      if (!submitted) {
        // 작업 제출 자체가 실패해도 라운드는 이미 닫혔다 — 표를 정리해 재투표로 복구 가능하게 한다
        questionVoteStore.clearVotes(chatroomId);
        questionVoteStore.endGeneration(chatroomId);
      }
    }
  }

  /** 질문을 생성·저장하고 broadcast까지 시도했으면 true. false면 이번 라운드는 무효. */
  private boolean generate(Long chatroomId) {
    ChatRoom chatRoom = chatRoomRepository.findByIdWithMeetingAndBook(chatroomId).orElse(null);
    if (chatRoom == null || chatRoom.getMeeting().getStatus() != MeetingStatus.IN_PROGRESS) {
      return false;
    }
    Meeting meeting = chatRoom.getMeeting();

    List<AIQuestion> previousQuestions =
        aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(meeting.getId());
    if (previousQuestions.size() >= MAX_QUESTIONS) {
      return false;
    }
    int nextOrder =
        previousQuestions.isEmpty()
            ? 1
            : previousQuestions.get(previousQuestions.size() - 1).getQuestionOrder() + 1;

    // LLM 호출은 트랜잭션(DB 커넥션) 밖에서 수행한다
    String content = generateContent(meeting, previousQuestions, nextOrder);

    AIQuestion saved;
    try {
      saved =
          transactionTemplate.execute(
              status ->
                  aiQuestionRepository.save(
                      AIQuestion.builder()
                          .meeting(meeting)
                          .content(content)
                          .questionOrder(nextOrder)
                          .build()));
    } catch (DataIntegrityViolationException e) {
      // (meeting_id, question_order) unique 제약 — 드물게 동시 생성이 겹치면 한쪽만 저장된다
      log.warn("AI 질문 동시 생성 충돌: meetingId={}, order={}", meeting.getId(), nextOrder);
      return false;
    }

    // 커밋이 끝난 뒤에만 라운드를 리셋하고 broadcast한다 —
    // 커밋 전에 하면 저장 실패 시 존재하지 않는 질문이 전파되고 표만 사라진다
    questionVoteStore.clearVotes(chatroomId);

    try {
      messagingTemplate.convertAndSend(
          ChatFrame.CHATROOM_SUB_DESTINATION + chatroomId,
          ChatFrame.of(
              ChatFrame.TYPE_QUESTION,
              chatroomId,
              AIConverter.toChatQuestionPayload(saved, MAX_QUESTIONS)));
    } catch (Exception e) {
      // 저장은 완료된 상태 — 프레임 유실 시 클라이언트는 재입장(입장 API)으로 최신 질문을 복구한다.
      // MVP에서는 outbox/재전송 없이 이 복구 경로를 계약으로 둔다.
      log.error(
          "QUESTION broadcast 실패: chatroomId={}, questionId={}", chatroomId, saved.getId(), e);
    }
    return true;
  }

  private String generateContent(
      Meeting meeting, List<AIQuestion> previousQuestions, int nextOrder) {
    try {
      return geminiQuestionClient.generateQuestion(
          meeting.getBook().getTitle(),
          meeting.getBook().getAuthor(),
          previousQuestions.stream().map(AIQuestion::getContent).toList());
    } catch (Exception e) {
      log.warn("Gemini 질문 생성 실패 — 기본 질문으로 폴백: meetingId={}", meeting.getId(), e);
      return FALLBACK_QUESTIONS.get((nextOrder - 1) % FALLBACK_QUESTIONS.size());
    }
  }
}
