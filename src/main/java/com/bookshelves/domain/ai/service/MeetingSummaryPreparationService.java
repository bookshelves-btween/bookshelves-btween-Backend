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

// 모임 종료 후 요약 3주제를 저장한다.
//
// 실패는 전부 안내 문구로 수렴한다. 키 미설정·타임아웃·파싱 실패·검증 탈락 어느 쪽이든 요약은 반드시
// 3행 저장된다. 프론트가 주제 3칸을 항상 그리기 때문이다.
@Slf4j
@Service
public class MeetingSummaryPreparationService {

  // 축에 쓸 내용이 없거나 생성에 실패했을 때 제목 자리에 들어간다. 본문은 비운다.
  static final String FALLBACK_TITLE = "나눈 이야기가 적어 정리하지 못했어요";

  // 총 4회 시도(최초 1회 + 재시도 3회). 일시적 오류로 영구적인 안내 문구가 박히는 것을 막는다.
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
      // WebSocket 브로커 설정이 채널용 TaskExecutor 빈을 여럿 등록하므로 전용 빈을 명시 지정한다
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

  // 종료 트랜잭션이 커밋된 뒤에만 시작한다 — 롤백된 종료로 요약을 만들지 않기 위함.
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
        // 요약은 있는데 알림 직전에 중단된 경우가 있다. 알림 생성은 멱등이므로 그냥 호출한다.
        meetingSummaryNotifier.notifySummaryDone(meetingId);
        return;
      }
      Meeting meeting = meetingRepository.findWithBookById(meetingId).orElse(null);
      if (meeting == null) {
        return; // 종료 직후 삭제된 모임
      }

      Map<SummaryAxis, SummaryDraft> drafts = generate(meeting);
      saveSummaries(meetingId, drafts);

      // 저장 성공 여부를 결과로 확인한 뒤에만 알림을 만든다.
      // 예외 종류로 판정하면 unique 충돌과 길이 초과를 구분하지 못해, 한 행도 안 들어간 모임에
      // 요약이 준비되었다는 알림을 보내게 된다.
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

  // 개수가 아니라 축의 존재로 판정한다 — 개수만 보면 축이 어긋난 데이터에서 준비 완료로 오판한다.
  private boolean isFullyPrepared(Long meetingId) {
    return existingAxes(meetingId).size() == SummaryAxis.count();
  }

  private Set<SummaryAxis> existingAxes(Long meetingId) {
    return meetingSummaryRepository.findAllByMeetingId(meetingId).stream()
        .map(MeetingSummary::getAxis)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(SummaryAxis.class)));
  }

  // 락 밖에서 호출한다 — 응답이 오래 걸리므로 락을 쥔 채 기다리지 않는다.
  private Map<SummaryAxis, SummaryDraft> generate(Meeting meeting) {
    List<ChatMessage> messages =
        chatRoomRepository
            .findByMeetingId(meeting.getId())
            .map(chatRoom -> chatMessageRepository.findAllWithSenderByChatroomId(chatRoom.getId()))
            .orElseGet(List::of);
    if (messages.isEmpty()) {
      // 요약할 재료가 없다. 호출 비용을 쓰지 않고 바로 안내 문구로 채운다.
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

  // 다시 보내면 결과가 달라질 수 있는 오류만 재시도한다.
  //
  // 모델 혼잡(5xx), 무료 티어 쿼터(429), 네트워크·타임아웃이 여기 해당한다. 잘못된 요청이나 인증
  // 실패는 몇 번을 보내도 같은 응답이 오므로 백오프만 태우고 전용 executor 스레드를 붙잡는다.
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

  // 저장 시점에 모임 행 락을 잡고, 락 안에서 기존 축을 다시 조회한다.
  //
  // 진입 가드만으로는 부족하다. 중복 작업 두 개가 가드를 함께 통과한 뒤 차례로 락을 얻으면, 두 번째가
  // 락 안에서 다시 확인하지 않는 한 세 행을 또 INSERT해 unique 위반으로 끝난다.
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
      // 락이 닿지 않는 경로(다중 인스턴스)에서의 unique 충돌과, 길이 초과·NOT NULL 위반이 모두 같은
      // 예외형으로 온다. 여기서는 구분하지 않고 로그만 남기고, 실제 성공 여부는 호출부가 저장된 축을
      // 다시 세어 판정한다.
      log.warn("AI 요약 저장 충돌 또는 제약 위반: meetingId={}", meetingId, e);
    }
  }

  // 아직 없는 축만 만든다. 초안이 없거나 본문이 비면 안내 문구로 채운다 — 축별로 독립 폴백된다.
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
