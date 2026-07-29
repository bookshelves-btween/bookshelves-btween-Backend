package com.bookshelves.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

// 모임 진행 공통 질문(시드). 모든 모임이 이 5개를 같은 순서로 사용한다.
//
// 순서 자체가 기획된 흐름이다 — 전/후 감상 → 별점 → 인상 깊은 장면 → 기억하고 싶은 문장 → 한 문장 소개로
// 가벼운 감상에서 깊은 성찰로 올라간다. LLM 자유 생성은 이 흐름을 매번 흔들고 질문이 겹치므로,
// 흐름은 시드로 고정하고 LLM은 "책에 맞게 구체화"만 담당한다(AIQuestionPreparationService).
//
// 각색에 실패하거나 책 정보가 부족하면 이 원문이 그대로 쓰인다 — 시드가 곧 폴백이다.
@Getter
public enum SeedQuestion {
  READING_IMPRESSION(1, "작품을 읽기 전과 후의 감상을 각각 이야기해주세요."),
  RATING(2, "작품에 대한 별점을 공유해주세요. 단편집이라면 가장 높은 별점의 작품과, 가장 낮은 별점의 작품을 공유해주세요."),
  MEMORABLE_SCENE(3, "작품을 읽으며 가장 오래 마음에 남은 장면은 무엇이었나요?"),
  MEMORABLE_SENTENCE(4, "작품 속에서 기억에 남거나 기억하고 싶은 문장이 있나요?"),
  ONE_LINE_PITCH(5, "이 책을 아직 읽지 않은 사람에게 한 문장으로 소개한다면, 어떻게 표현하고 싶나요?");

  private final int questionOrder;
  private final String content;

  SeedQuestion(int questionOrder, String content) {
    this.questionOrder = questionOrder;
    this.content = content;
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
