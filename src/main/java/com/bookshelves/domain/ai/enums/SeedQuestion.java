package com.bookshelves.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

// 모든 모임에 공통으로 적용되는 질문 순서와 폴백 문장.
// content는 생성 실패 시 사용하고, intent는 질문 생성 프롬프트에 전달한다.
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

  /** 모임당 질문 수. */
  public static int count() {
    return values().length;
  }

  /** questionOrder 오름차순 목록. */
  public static List<SeedQuestion> ordered() {
    return Arrays.stream(values())
        .sorted((left, right) -> Integer.compare(left.questionOrder, right.questionOrder))
        .toList();
  }

  /** 모임에 필요한 모든 questionOrder. */
  public static Set<Integer> allOrders() {
    return Arrays.stream(values()).map(SeedQuestion::getQuestionOrder).collect(Collectors.toSet());
  }
}
