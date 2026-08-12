package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.book.entity.Book;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

// 시드 질문의 의도를 유지하면서 책에 맞는 모임 질문 다섯 개를 생성한다.
// 검증에서 제외된 순서는 호출부가 시드 질문으로 채운다.
@Slf4j
@Component
public class GeminiQuestionClient {

  // 시간 초과 시 호출부가 시드 질문으로 진행할 수 있도록 짧게 제한한다.
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

  // 생성 문장의 길이는 시드 질문과 무관하므로 절대값으로 검증한다.
  private static final int MAX_LENGTH = 100;
  private static final int MIN_LENGTH = 10;

  private final GeminiClient geminiClient;

  @Autowired
  public GeminiQuestionClient(GeminiClientFactory geminiClientFactory) {
    this(geminiClientFactory.create(CONNECT_TIMEOUT, READ_TIMEOUT));
  }

  GeminiQuestionClient(GeminiClient geminiClient) {
    this.geminiClient = geminiClient;
  }

  /**
   * 질문 다섯 개를 LLM 1회 호출로 생성한다.
   *
   * <p>소개가 비어 있어도 호출한다. 모델이 제목·저자로 책을 알아볼 수 있고, 못 알아보더라도 자리의 의도만으로 시드보다 나은 문장이 나올 수 있다.
   *
   * @return question_order → 생성된 질문. 검증을 통과한 항목만 담긴다.
   */
  public Map<Integer, String> generateQuestions(Book book) {
    return validate(geminiClient.generate(buildPrompt(book), new TypeReference<>() {}));
  }

  String buildPrompt(Book book) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("당신은 독서 모임 진행자입니다. 아래 책으로 나눌 대화 질문 5개를 만들어 주세요.\n\n").append("[책 정보]\n");
    Prompts.appendBookInfo(prompt, book);

    // 시드 문장의 단순 변형을 피하기 위해 원문 대신 의도만 전달한다.
    prompt.append("\n[질문 자리] 순서와 의도는 고정입니다. 문장은 이 책에 맞게 처음부터 새로 쓰세요.\n");
    SeedQuestion.ordered()
        .forEach(
            seed ->
                prompt
                    .append(seed.getQuestionOrder())
                    .append(". ")
                    .append(seed.getIntent())
                    .append('\n'));

    prompt
        .append("\n[규칙]\n")
        .append("1. 이 책을 확실히 안다면 실제 인물·사건·주제를 끌어와 구체적으로 물으세요. ")
        .append("확실하지 않다면 책의 구체적인 내용을 아예 언급하지 말고, ")
        .append("이 책에도 자연스럽게 들어맞는 담백한 질문으로 쓰세요. ")
        .append("어설프게 아는 내용을 끼워넣는 것이 가장 나쁩니다.\n")
        .append("2. 참가자가 자기 해석과 경험을 말할 수 있는 열린 질문으로 쓰세요. ")
        .append("정답이 정해진 퀴즈나 줄거리 확인 질문은 만들지 마세요.\n")
        .append("3. 줄거리를 요약해 알려주지 마세요. 질문이지 설명이 아닙니다.\n")
        .append("4. \"~를 다룬 이 작품을 읽고\", \"~한 이야기를 담은 이 책에서\"처럼 ")
        .append("수식어를 앞에 붙이는 방식으로 쓰지 마세요.\n")
        .append("5. 한국어로, 각 질문은 한두 문장이며 ")
        .append(MAX_LENGTH)
        .append("자를 넘기지 마세요.\n\n")
        .append("[출력 형식] 다른 설명 없이 아래 JSON 배열만 출력하세요.\n")
        .append("[{\"order\": 1, \"question\": \"...\"}, ...]");
    return prompt.toString();
  }

  // 순서와 길이만 검증하며, 책 내용의 사실 여부는 판정하지 않는다.
  private Map<Integer, String> validate(List<GeneratedQuestion> candidates) {
    Set<Integer> knownOrders = SeedQuestion.allOrders();

    Map<Integer, String> accepted = new LinkedHashMap<>();
    for (GeneratedQuestion candidate : candidates) {
      if (candidate == null || candidate.order() == null) {
        continue;
      }
      if (!knownOrders.contains(candidate.order()) || accepted.containsKey(candidate.order())) {
        continue; // 모르는 순서이거나 중복 — 먼저 온 것만 채택
      }
      String question = Prompts.normalize(candidate.question());
      if (question == null || question.length() < MIN_LENGTH || question.length() > MAX_LENGTH) {
        log.warn(
            "생성 질문이 길이 검증에 걸려 시드 원문을 사용한다: order={}, 길이={}, 허용={}, 질문={}",
            candidate.order(),
            question == null ? 0 : question.length(),
            MAX_LENGTH,
            question);
        continue;
      }
      accepted.put(candidate.order(), question);
    }
    return accepted;
  }

  // 형식 밖의 필드 때문에 응답 전체가 폐기되지 않도록 무시한다.
  @JsonIgnoreProperties(ignoreUnknown = true)
  record GeneratedQuestion(Integer order, String question) {}
}
