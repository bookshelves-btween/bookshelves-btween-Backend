package com.bookshelves.domain.ai.enums;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

// 모임 요약의 분석 축. 요약은 이 세 축을 하나씩 반영한 주제 3개로 구성된다.
//
// 축은 서버 내부 개념이다. 응답에는 싣지 않는다 — 화면이 제목과 본문만 보여주므로 프론트가 축을
// 구분할 필요가 없다. 대신 두 가지 역할을 한다. (meeting_id, axis) unique 제약이 모임당 3행을
// 보장하는 근거이고, displayOrder가 항상 같은 순서로 내보내는 정렬 기준이다.
//
// description은 프롬프트에 그대로 들어간다. 모델이 각 축에서 무엇을 뽑아야 하는지 알려주는 설명이다.
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

  /** displayOrder 오름차순 목록. enum 선언 순서에 의존하지 않도록 명시 정렬한다. */
  public static List<SummaryAxis> ordered() {
    return Arrays.stream(values())
        .sorted((left, right) -> Integer.compare(left.displayOrder, right.displayOrder))
        .toList();
  }

  /** 모임당 저장되어야 하는 요약 행 수. */
  public static int count() {
    return values().length;
  }
}
