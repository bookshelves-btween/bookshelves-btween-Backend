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

// 모임 질문 다섯 개를 그 책에 맞게 새로 쓴다.
//
// 예전에는 시드 원문을 프롬프트에 주고 각색시켰다. 그러면 모델이 원문을 보존하려 들면서 앞에 수식어만
// 붙이는 결과로 수렴한다. 감상을 묻는 질문 앞에 줄거리 요약을 덧대는 식이라 원문보다 길기만 하고
// 나아진 게 없다. 그래서 시드 문장은 프롬프트에서 빼고, 자리의 의도(SeedQuestion.intent)만 주고
// 문장은 처음부터 쓰게 한다.
//
// 모델의 내부 지식 사용을 허용한다. 대신 아는 척을 금지한다. 확실하지 않으면 책의 구체적 내용을 아예
// 언급하지 말고 담백한 질문을 쓰게 한다. 어설프게 아는 내용을 끼워넣는 것이 가장 나쁜 결과이기 때문이다.
//
// 반환값은 검증을 통과한 항목만 담는다. 빠진 순서는 호출부가 시드 원문으로 채운다.
@Slf4j
@Component
public class GeminiQuestionClient {

  // LLM 응답 지연이 준비 스레드를 계속 붙잡지 않도록 짧게 끊는다. 초과하면 시드 원문으로 진행한다.
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

  // 길이는 시드 대비 비율이 아니라 절대값으로 잰다.
  //
  // 문장을 새로 쓰게 한 이상 시드 길이는 기준이 될 수 없다. 비율로 재던 시절에는 시드가 28자로 짧은 1번에서
  // 멀쩡한 질문이 한 글자 차이로 잘려나갔다. MAX_LENGTH는 프롬프트에 적는 상한과 같은 값을 쓴다.
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

    // 시드 원문이 아니라 자리의 의도만 준다. 원문을 보여주면 모델이 그 문장을 보존하려 든다.
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

  // LLM 응답을 그대로 믿지 않는다. 구조를 어긴 항목은 버리고 호출부가 시드 원문을 쓰게 한다.
  //
  // 내용 검증은 하지 않는다. 모델 지식 사용을 허용한 이상 책에 실제로 있는 내용인지를 우리가 판정할
  // 수단이 없고, 흉내만 낸 검증은 통과시키면 안 되는 것을 통과시키면서 멀쩡한 것만 떨어뜨린다.
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
        // 버린 문장까지 남긴다. 길이 상한이 실제로 어디서 걸리는지 로그만 보고 판단할 수 있어야 한다.
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

  // 모델이 형식 밖의 필드를 덧붙여도 파싱 전체가 실패하지 않도록 미지 필드를 무시한다.
  // 필드 하나 때문에 질문 5개가 전부 시드 원문으로 떨어지는 것을 막는다.
  @JsonIgnoreProperties(ignoreUnknown = true)
  record GeneratedQuestion(Integer order, String question) {}
}
