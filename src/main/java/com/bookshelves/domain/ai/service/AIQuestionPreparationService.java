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

// 모임의 AI 질문 5개를 모임 시작 전에 미리 저장한다.
//
// 모집 마감(모임 성립 확정) 시점에 LLM을 1회 호출해 그 책에 맞는 질문을 만들고,
// question_order 1~5로 저장한다. 모임 시작 시점에는 커서만 1로 올리면 되므로 채팅 중 LLM 대기가 없다.
//
// 실패는 전부 "시드 원문 사용"으로 수렴한다 — 키 미설정·타임아웃·파싱 실패·검증 탈락 어느 쪽이든
// 질문은 반드시 5개 저장된다. LLM 생성은 품질 향상이지 기능의 전제가 아니다.
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
      // WebSocket 브로커 설정이 채널용 TaskExecutor 빈을 여럿 등록하므로 전용 빈을 명시 지정한다
      @Qualifier("aiQuestionTaskExecutor") TaskExecutor taskExecutor,
      TransactionTemplate transactionTemplate) {
    this.meetingRepository = meetingRepository;
    this.aiQuestionRepository = aiQuestionRepository;
    this.geminiQuestionClient = geminiQuestionClient;
    this.taskExecutor = taskExecutor;
    this.transactionTemplate = transactionTemplate;
  }

  // 모집 마감 트랜잭션이 커밋된 뒤에만 준비를 시작한다 — 롤백된 마감으로 질문을 만들지 않기 위함.
  // LLM 호출은 커밋 후에도 별도 스레드로 넘겨 마감 처리(스케줄러/참여 API) 응답을 붙잡지 않는다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecruitClosed(MeetingRecruitClosedEvent event) {
    try {
      taskExecutor.execute(() -> prepare(event.meetingId()));
    } catch (Exception e) {
      // 작업 제출 실패는 여기서 끝낸다 — 모임 시작 시 안전망(ensureSeeded)이 원문으로 채운다
      log.error("AI 질문 준비 작업 제출 실패: meetingId={}", event.meetingId(), e);
    }
  }

  /** 생성을 시도해 질문 5개를 저장한다. 이미 다 저장돼 있으면 아무것도 하지 않는다. */
  public void prepare(Long meetingId) {
    try {
      if (isFullyPrepared(meetingId)) {
        return; // 재실행·중복 이벤트에 대한 멱등 가드
      }
      Meeting meeting = meetingRepository.findWithBookById(meetingId).orElse(null);
      if (meeting == null) {
        return; // 마감 직후 삭제된 모임
      }

      Map<Integer, String> generated = generate(meeting);
      saveQuestions(meetingId, generated);
      log.info("AI 질문 준비 완료: meetingId={}, 생성={}건", meetingId, generated.size());
    } catch (Exception e) {
      log.error("AI 질문 준비 실패: meetingId={}", meetingId, e);
    }
  }

  /**
   * LLM 생성 없이 시드 원문으로 빠진 질문을 채운다. 호출한 트랜잭션 안에서 실행된다.
   *
   * <p>모집 마감 경로를 타지 않고 모임이 시작된 경우(정원 미충족 모임 등)의 안전망이다. 커서는 시작과 동시에 1로 올라가고 최대 5까지 오르므로, 1~5가 모두 있어야
   * 공개할 질문이 비지 않는다.
   */
  public void ensureSeeded(Long meetingId) {
    // 호출자(모임 시작)가 이미 같은 행을 잠그고 있지만, 불변식을 호출자에 의존시키지 않으려 여기서도 잡는다.
    // 비동기 준비와 같은 락 아래로 들어와야 양쪽이 동시에 "없음"을 읽고 둘 다 INSERT하는 상황이 막힌다.
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

  // 개수가 아니라 순서 1~5가 모두 있는지로 판정한다 — 개수만 보면 순서가 어긋난 데이터에서
  // "준비 완료"로 오판해 생성도 보충도 하지 않고 넘어간다
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

  // 생성이 끝난 뒤 저장 시점에 모임 행 락을 잡고, 그 안에서 빠진 순서를 다시 계산한다.
  //
  // 안전망(ensureSeeded)도 같은 행 락 아래에서 돈다. 락 없이 저장하면 생성이 도는 사이에
  // 시작 스케줄러가 끼어들어 양쪽이 "없음"을 읽고 둘 다 INSERT할 수 있고, 이때 unique 제약에
  // 걸리는 쪽이 안전망이면 모임 시작 트랜잭션이 통째로 롤백된다.
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
      // 락이 닿지 않는 경로(다중 인스턴스)까지 대비한 최종 방어 — (meeting_id, question_order) unique
      log.warn("AI 질문 동시 준비 충돌: meetingId={}", meetingId);
    }
  }

  // 아직 없는 순서만 만든다.
  //
  // "한 행이라도 있으면 준비 완료"로 보면 1~4개만 남은 모임(구버전 데이터, 부분 정리 등)이 그대로 시작되고,
  // 커서는 5까지 오르므로 존재하지 않는 순서를 가리켜 질문이 표시되지 않는다.
  // 생성본이 있는 순서만 교체하고 나머지는 시드 원문을 쓴다 — 항목별로 독립 폴백된다.
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
