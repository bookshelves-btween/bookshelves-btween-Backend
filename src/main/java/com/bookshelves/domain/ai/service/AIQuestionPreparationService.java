package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.client.GeminiQuestionClient;
import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.event.MeetingRecruitClosedEvent;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

// 모집 마감 후 질문 다섯 개를 미리 생성해 모임 중 LLM 대기를 없앤다.
// 생성하지 못한 질문은 시드 문장으로 채운다.
@Slf4j
@Service
public class AIQuestionPreparationService {

  private final MeetingRepository meetingRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final GeminiQuestionClient geminiQuestionClient;
  private final TaskExecutor taskExecutor;
  private final TransactionTemplate transactionTemplate;

  public AIQuestionPreparationService(
      MeetingRepository meetingRepository,
      AIQuestionRepository aiQuestionRepository,
      GeminiQuestionClient geminiQuestionClient,
      // 같은 타입의 WebSocket 실행기와 구분한다.
      @Qualifier("aiQuestionTaskExecutor") TaskExecutor taskExecutor,
      TransactionTemplate transactionTemplate) {
    this.meetingRepository = meetingRepository;
    this.aiQuestionRepository = aiQuestionRepository;
    this.geminiQuestionClient = geminiQuestionClient;
    this.taskExecutor = taskExecutor;
    this.transactionTemplate = transactionTemplate;
  }

  // 모집 마감 커밋 후 전용 실행기에서 준비한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecruitClosed(MeetingRecruitClosedEvent event) {
    try {
      taskExecutor.execute(() -> prepare(event.meetingId()));
    } catch (Exception e) {
      log.error("AI 질문 준비 작업 제출 실패: meetingId={}", event.meetingId(), e);
    }
  }

  /** 질문 다섯 개를 준비하며, 이미 준비된 모임은 건너뛴다. */
  public void prepare(Long meetingId) {
    try {
      if (isFullyPrepared(meetingId)) {
        return;
      }
      Meeting meeting = meetingRepository.findWithBookById(meetingId).orElse(null);
      if (meeting == null) {
        return;
      }

      Map<Integer, String> generated = generate(meeting);
      saveQuestions(meetingId, generated);
      log.info("AI 질문 준비 완료: meetingId={}, 생성={}건", meetingId, generated.size());
    } catch (Exception e) {
      log.error("AI 질문 준비 실패: meetingId={}", meetingId, e);
    }
  }

  /** 모임 시작 시 빠진 질문을 시드 문장으로 채우는 안전망. */
  public void ensureSeeded(Long meetingId) {
    // 비동기 준비와 같은 행 락을 사용해 중복 INSERT를 막는다.
    meetingRepository
        .findByIdForUpdate(meetingId)
        .ifPresent(
            meeting -> {
              List<AIQuestion> missing = buildMissingQuestions(meeting, Map.of());
              if (missing.isEmpty()) {
                return;
              }
              log.warn(
                  "모집 마감 시 AI 질문이 준비되지 않아 시드 원문으로 채운다: meetingId={}, 보충={}개",
                  meetingId,
                  missing.size());
              aiQuestionRepository.saveAll(missing);
            });
  }

  // 개수가 아닌 필수 순서의 존재 여부로 준비 상태를 판단한다.
  private boolean isFullyPrepared(Long meetingId) {
    return existingOrders(meetingId).containsAll(SeedQuestion.allOrders());
  }

  private Set<Integer> existingOrders(Long meetingId) {
    return aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(meetingId).stream()
        .map(AIQuestion::getQuestionOrder)
        .collect(Collectors.toSet());
  }

  private Map<Integer, String> generate(Meeting meeting) {
    try {
      return geminiQuestionClient.generateQuestions(meeting.getBook());
    } catch (Exception e) {
      log.warn("Gemini 질문 생성 실패 — 시드 원문을 사용한다: meetingId={}", meeting.getId(), e);
      return Map.of();
    }
  }

  // LLM 호출 밖에서 모임 행을 잠그고 누락된 순서를 다시 계산한다.
  private void saveQuestions(Long meetingId, Map<Integer, String> generated) {
    try {
      transactionTemplate.executeWithoutResult(
          status ->
              meetingRepository
                  .findByIdForUpdate(meetingId)
                  .ifPresent(
                      meeting -> {
                        List<AIQuestion> missing = buildMissingQuestions(meeting, generated);
                        if (!missing.isEmpty()) {
                          aiQuestionRepository.saveAll(missing);
                        }
                      }));
    } catch (DataIntegrityViolationException e) {
      // 다중 인스턴스의 경합은 unique 제약으로 막는다.
      log.warn("AI 질문 동시 준비 충돌: meetingId={}", meetingId);
    }
  }

  // 누락된 순서만 생성하며 사용할 생성본이 없으면 해당 시드 문장으로 채운다.
  private List<AIQuestion> buildMissingQuestions(Meeting meeting, Map<Integer, String> generated) {
    Set<Integer> existingOrders = existingOrders(meeting.getId());

    return SeedQuestion.ordered().stream()
        .filter(seed -> !existingOrders.contains(seed.getQuestionOrder()))
        .map(
            seed ->
                AIQuestion.builder()
                    .meeting(meeting)
                    .content(generated.getOrDefault(seed.getQuestionOrder(), seed.getContent()))
                    .questionOrder(seed.getQuestionOrder())
                    .build())
        .toList();
  }
}
