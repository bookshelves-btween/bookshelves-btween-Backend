package com.bookshelves.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

// 모임 요약을 구성하는 세 가지 분석 축.
// description은 생성 프롬프트에, displayOrder는 응답 정렬에 사용한다.
@Getter
public enum SummaryAxis {
  KEY_ARGUMENT(
      1, "작품의 핵심 논점. 책의 핵심 주제 또는 모임에서 반복적으로 논의된 질문." + " 줄거리 요약이 아니라 참여자들이 해석하고 토론한 내용을 우선합니다."),
  REACTION(
      2, "참여자들의 주요 반응과 해석. 공통적으로 인상 깊게 느낀 내용," + " 평가가 갈리거나 다양한 해석이 나온 내용, 별점·장면·문장·주장에 대한 반응."),
  LIFE_LINK(
      3, "독자의 삶과 연결된 지점. 책과 참여자의 경험·가치관·고민이 연결된 내용," + " 책을 읽기 전후 달라진 생각, 실제 삶에 적용하거나 확장해 본 내용.");

  private final int displayOrder;
  private final String description;

  SummaryAxis(int displayOrder, String description) {
    this.displayOrder = displayOrder;
    this.description = description;
  }

  /** displayOrder 오름차순 목록. */
  public static List<SummaryAxis> ordered() {
    return Arrays.stream(values())
        .sorted((left, right) -> Integer.compare(left.displayOrder, right.displayOrder))
        .toList();
  }

  /** 모임당 요약 행 수. */
  public static int count() {
    return values().length;
  }
}
