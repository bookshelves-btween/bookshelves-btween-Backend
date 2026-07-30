package com.bookshelves.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

// 모임 진행 질문 다섯 자리. 모든 모임이 이 순서를 그대로 쓴다.
//
// 순서 자체가 기획된 흐름이다 — 전/후 감상 → 별점 → 인상 깊은 장면 → 기억하고 싶은 문장 → 한 문장 소개로
// 가벼운 감상에서 깊은 성찰로 올라간다. LLM에 흐름까지 맡기면 매 모임 구성이 달라지고 질문이 겹치므로,
// 흐름은 여기서 고정하고 문장만 LLM이 그 책에 맞게 새로 쓴다(GeminiQuestionClient).
//
// 그래서 값이 둘이다. content는 LLM이 실패했을 때 그대로 쓰이는 폴백 문장이고,
// intent는 프롬프트에 넘겨 이 자리에서 무엇을 물어야 하는지 알려주는 설명이다.
// 프롬프트에 content를 넣지 않는 것이 중요하다 — 원문을 보여주면 모델이 그 문장을 보존하려 들면서
// 앞에 수식어만 붙이는 결과로 수렴한다.
@Getter
public enum SeedQuestion {
  READING_IMPRESSION(
      1, "작품을 읽기 전과 후의 감상을 각각 이야기해주세요.", "읽기 전의 예상과 다 읽은 뒤의 인상이 어떻게 달라졌는지 — 가볍게 문을 여는 자리"),
  RATING(
      2,
      "작품에 대한 별점을 공유해주세요. 단편집이라면 가장 높은 별점의 작품과, 가장 낮은 별점의 작품을 공유해주세요.",
      "별점과 그 점수를 가른 이유 — 평가를 수치와 근거로 꺼내는 자리"),
  MEMORABLE_SCENE(3, "작품을 읽으며 가장 오래 마음에 남은 장면은 무엇이었나요?", "가장 오래 마음에 남은 장면 — 개인의 감정이 걸린 지점을 묻는 자리"),
  MEMORABLE_SENTENCE(4, "작품 속에서 기억에 남거나 기억하고 싶은 문장이 있나요?", "기억에 남거나 남기고 싶은 문장 — 텍스트 자체로 돌아가는 자리"),
  ONE_LINE_PITCH(
      5,
      "이 책을 아직 읽지 않은 사람에게 한 문장으로 소개한다면, 어떻게 표현하고 싶나요?",
      "아직 읽지 않은 사람에게 건네는 한 문장 소개 — 대화를 닫으며 정리하는 자리");

  private final int questionOrder;
  private final String content;
  private final String intent;

  SeedQuestion(int questionOrder, String content, String intent) {
    this.questionOrder = questionOrder;
    this.content = content;
    this.intent = intent;
  }

  /** 모임당 질문 수 = 시드 개수. 명세의 maxQuestions와 같은 값이다. */
  public static int count() {
    return values().length;
  }

  /** question_order(1부터) 오름차순 목록. enum 선언 순서에 의존하지 않도록 명시 정렬한다. */
  public static List<SeedQuestion> ordered() {
    return Arrays.stream(values())
        .sorted((left, right) -> Integer.compare(left.questionOrder, right.questionOrder))
        .toList();
  }

  /** 모임에 반드시 존재해야 하는 question_order 집합. */
  public static Set<Integer> allOrders() {
    return Arrays.stream(values()).map(SeedQuestion::getQuestionOrder).collect(Collectors.toSet());
  }
}
