package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.book.entity.Book;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

// 오늘의 추천 도서에 붙일 한 줄 멘트를 쓴다.
//
// 홈 조회 시점이 아니라 전날 23시 스케줄러에서 한 번 부른다. 홈은 트래픽이 가장 많은 화면이라
// 조회마다 LLM을 태우면 응답 시간과 장애 영향이 그대로 노출된다.
//
// 실패해도 예외를 올린다. 폴백 문구를 여기서 만들지 않는 이유는, 무엇으로 폴백할지가 책 소개를 아는
// 호출부의 판단이기 때문이다. 여기서 기본 문구를 반환하면 생성 실패와 성공을 호출부가 구분할 수 없다.
@Slf4j
@Component
public class GeminiRecommendationClient {

  // 멘트 한 줄이라 응답이 짧다. 스케줄러가 새벽까지 물고 있을 이유가 없다.
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  // 프롬프트로 요구하는 길이와 코드로 자르는 길이를 다르게 둔다.
  //
  // 모델은 토큰 단위로 처리해서 한글 글자 수를 셀 수 없다. 몇 자로 쓰라고 못박아도 몇 글자씩 넘나드는
  // 것이 정상이다. 요구 범위를 그대로 하드 컷으로 쓰면 멀쩡한 문장이 두 글자 차이로 버려지고 폴백만
  // 늘어난다. 그래서 프롬프트에는 목표 범위를 주고, 코드는 화면이 실제로 감당하는 선에서만 자른다.
  private static final int TARGET_MIN_LENGTH = 15;
  private static final int TARGET_MAX_LENGTH = 30;
  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 40;

  private final GeminiClient geminiClient;

  @Autowired
  public GeminiRecommendationClient(GeminiClientFactory geminiClientFactory) {
    this(geminiClientFactory.create(CONNECT_TIMEOUT, READ_TIMEOUT));
  }

  GeminiRecommendationClient(GeminiClient geminiClient) {
    this.geminiClient = geminiClient;
  }

  /**
   * 추천 멘트 한 줄을 생성한다.
   *
   * @return 검증을 통과한 멘트. 생성물이 검증을 통과하지 못하면 null을 돌려주고 호출부가 폴백한다.
   */
  public String generateMessage(Book book) {
    List<GeneratedMessage> candidates =
        geminiClient.generate(buildPrompt(book), new TypeReference<>() {});

    for (GeneratedMessage candidate : candidates) {
      if (candidate == null) {
        continue;
      }
      String message = Prompts.normalize(candidate.message());
      if (message == null || message.length() < MIN_LENGTH || message.length() > MAX_LENGTH) {
        log.warn(
            "추천 멘트가 길이 검증에 걸려 폴백한다: bookId={}, 길이={}, 허용={}~{}, 멘트={}",
            book.getId(),
            message == null ? 0 : message.length(),
            MIN_LENGTH,
            MAX_LENGTH,
            message);
        continue;
      }
      return message;
    }
    return null;
  }

  String buildPrompt(Book book) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("당신은 독서 앱의 카피라이터입니다. 아래 책을 오늘의 추천 도서로 소개하는 한 줄 멘트를 써 주세요.\n\n")
        .append("[책 정보]\n");
    Prompts.appendBookInfo(prompt, book);

    // 지어내지 말라는 규칙을 1번에 둔다. 카피라이터 역할을 주면 모델이 근거보다 분위기를 우선하기 쉬운데,
    // 추천 멘트는 책 내용을 단정하는 문장이라 틀리면 사용자가 바로 알아챈다.
    prompt
        .append("\n[규칙]\n")
        .append("1. 소개에 없는 내용이나 확실하지 않은 줄거리를 지어내지 마세요. ")
        .append("아는 것이 제목과 분류뿐이라면 책의 구체적인 내용을 언급하지 말고 ")
        .append("읽고 싶어지는 담백한 한 줄로 쓰세요.\n")
        .append("2. 줄거리를 요약하지 마세요. 소개가 아니라 오늘 이 책을 펼치고 싶게 만드는 문장입니다.\n")
        .append("3. 공백을 포함해 ")
        .append(TARGET_MIN_LENGTH)
        .append("자에서 ")
        .append(TARGET_MAX_LENGTH)
        .append("자 사이로 쓰세요.\n")
        .append("4. 한국어 한 문장으로 쓰고 문장 끝에 마침표를 붙이지 마세요.\n")
        .append("5. 책 제목을 그대로 반복하지 마세요. 제목은 멘트 옆에 이미 표시됩니다.\n\n")
        .append("[출력 형식] 다른 설명 없이 아래 JSON 배열만 출력하세요. 항목은 하나입니다.\n")
        .append("[{\"message\": \"...\"}]");
    return prompt.toString();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record GeneratedMessage(String message) {}
}
