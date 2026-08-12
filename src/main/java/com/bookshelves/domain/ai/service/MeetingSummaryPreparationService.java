package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.client.GeminiSummaryClient;
import com.bookshelves.domain.ai.client.GeminiSummaryClient.SummaryDraft;
import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingTerminationService.MeetingEndedEvent;
import java.util.EnumSet;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

// 모임 종료 후 세 가지 축의 요약을 저장한다.
// 생성하지 못한 축도 안내 문구로 채워 항상 세 행을 유지한다.
@Slf4j
@Service
public class MeetingSummaryPreparationService {

  // 요약할 내용이 없거나 생성에 실패한 축의 제목.
  static final String FALLBACK_TITLE = "나눈 이야기가 적어 정리하지 못했어요";

  // 일시적 오류는 최초 호출을 포함해 네 번 시도한다.
  private static final int MAX_ATTEMPTS = 4;
  private static final long RETRY_BACKOFF_MILLIS = 3_000L;

  private final MeetingRepository meetingRepository;
  private final MeetingSummaryRepository meetingSummaryRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final GeminiSummaryClient geminiSummaryClient;
  private final MeetingSummaryNotifier meetingSummaryNotifier;
  private final TaskExecutor taskExecutor;
  private final TransactionTemplate transactionTemplate;

  public MeetingSummaryPreparationService(
      MeetingRepository meetingRepository,
      MeetingSummaryRepository meetingSummaryRepository,
      AIQuestionRepository aiQuestionRepository,
      ChatRoomRepository chatRoomRepository,
      ChatMessageRepository chatMessageRepository,
      GeminiSummaryClient geminiSummaryClient,
      MeetingSummaryNotifier meetingSummaryNotifier,
      // 같은 타입의 WebSocket 실행기와 구분한다.
      @Qualifier("meetingSummaryTaskExecutor") TaskExecutor taskExecutor,
      TransactionTemplate transactionTemplate) {
    this.meetingRepository = meetingRepository;
    this.meetingSummaryRepository = meetingSummaryRepository;
    this.aiQuestionRepository = aiQuestionRepository;
    this.chatRoomRepository = chatRoomRepository;
    this.chatMessageRepository = chatMessageRepository;
    this.geminiSummaryClient = geminiSummaryClient;
    this.meetingSummaryNotifier = meetingSummaryNotifier;
    this.taskExecutor = taskExecutor;
    this.transactionTemplate = transactionTemplate;
  }

  // 모임 종료 커밋 후 전용 실행기에서 준비한다.
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onMeetingEnded(MeetingEndedEvent event) {
    try {
      taskExecutor.execute(() -> prepare(event.meetingId()));
    } catch (Exception e) {
      log.error("AI 요약 준비 작업 제출 실패: meetingId={}", event.meetingId(), e);
    }
  }

  /** 요약 3행을 저장하고 완료 알림을 남긴다. 이미 다 저장돼 있으면 알림만 보정한다. */
  public void prepare(Long meetingId) {
    try {
      if (isFullyPrepared(meetingId)) {
        // 저장 후 알림 전에 중단된 경우를 보정한다.
        meetingSummaryNotifier.notifySummaryDone(meetingId);
        return;
      }
      Meeting meeting = meetingRepository.findWithBookById(meetingId).orElse(null);
      if (meeting == null) {
        return;
      }

      Map<SummaryAxis, SummaryDraft> drafts = generate(meeting);
      saveSummaries(meetingId, drafts);

      // 세 축이 모두 저장된 경우에만 완료 알림을 생성한다.
      if (!isFullyPrepared(meetingId)) {
        log.error("AI 요약 저장이 3행을 채우지 못해 알림을 보내지 않는다: meetingId={}", meetingId);
        return;
      }
      meetingSummaryNotifier.notifySummaryDone(meetingId);
      log.info("AI 요약 준비 완료: meetingId={}, 생성={}건", meetingId, drafts.size());
    } catch (Exception e) {
      log.error("AI 요약 준비 실패: meetingId={}", meetingId, e);
    }
  }

  // 중복을 제외한 축의 개수로 준비 상태를 판단한다.
  private boolean isFullyPrepared(Long meetingId) {
    return existingAxes(meetingId).size() == SummaryAxis.count();
  }

  private Set<SummaryAxis> existingAxes(Long meetingId) {
    return meetingSummaryRepository.findAllByMeetingId(meetingId).stream()
        .map(MeetingSummary::getAxis)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(SummaryAxis.class)));
  }

  // DB 락을 잡지 않은 상태에서 LLM을 호출한다.
  private Map<SummaryAxis, SummaryDraft> generate(Meeting meeting) {
    List<ChatMessage> messages =
        chatRoomRepository
            .findByMeetingId(meeting.getId())
            .map(chatRoom -> chatMessageRepository.findAllWithSenderByChatroomId(chatRoom.getId()))
            .orElseGet(List::of);
    if (messages.isEmpty()) {
      log.info("대화가 없어 LLM을 호출하지 않는다: meetingId={}", meeting.getId());
      return Map.of();
    }

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return geminiSummaryClient.generateSummaries(
            meeting.getBook(),
            aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(meeting.getId()),
            messages);
      } catch (Exception e) {
        if (attempt == MAX_ATTEMPTS || !isRetryable(e)) {
          log.warn("Gemini 요약 생성 실패 — 안내 문구를 사용한다: meetingId={}", meeting.getId(), e);
          return Map.of();
        }
        sleep(RETRY_BACKOFF_MILLIS * attempt);
      }
    }
    return Map.of();
  }

  // 서버 오류, 요청 제한, 네트워크 오류만 재시도한다.
  private boolean isRetryable(Exception e) {
    return e instanceof HttpServerErrorException
        || e instanceof HttpClientErrorException.TooManyRequests
        || e instanceof ResourceAccessException;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // 저장 시 모임 행을 잠그고 누락된 축을 다시 계산한다.
  private void saveSummaries(Long meetingId, Map<SummaryAxis, SummaryDraft> drafts) {
    try {
      transactionTemplate.executeWithoutResult(
          status ->
              meetingRepository
                  .findByIdForUpdate(meetingId)
                  .ifPresent(
                      meeting -> {
                        List<MeetingSummary> missing = buildMissingSummaries(meeting, drafts);
                        if (!missing.isEmpty()) {
                          meetingSummaryRepository.saveAll(missing);
                        }
                      }));
    } catch (DataIntegrityViolationException e) {
      // 실제 성공 여부는 호출부가 저장된 축을 다시 조회해 판단한다.
      log.warn("AI 요약 저장 충돌 또는 제약 위반: meetingId={}", meetingId, e);
    }
  }

  // 누락된 축만 생성하며 사용할 초안이 없으면 안내 문구로 채운다.
  private List<MeetingSummary> buildMissingSummaries(
      Meeting meeting, Map<SummaryAxis, SummaryDraft> drafts) {
    Set<SummaryAxis> existing = existingAxes(meeting.getId());

    return SummaryAxis.ordered().stream()
        .filter(axis -> !existing.contains(axis))
        .map(
            axis -> {
              SummaryDraft draft = drafts.get(axis);
              boolean usable = draft != null && draft.content() != null;
              return MeetingSummary.builder()
                  .meeting(meeting)
                  .axis(axis)
                  .title(usable ? draft.title() : FALLBACK_TITLE)
                  .content(usable ? draft.content() : null)
                  .build();
            })
        .toList();
  }
}
