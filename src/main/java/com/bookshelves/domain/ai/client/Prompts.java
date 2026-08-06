package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.book.entity.Book;

// 프롬프트 조립과 응답 정규화에서 클라이언트끼리 겹치는 부분.
final class Prompts {

  private Prompts() {}

  // 책 정보 블록은 용도가 달라도 같은 값을 같은 순서로 넣는다.
  //
  // 출간일과 ISBN은 동명이서와 개정판을 가르는 유일한 단서라서, 모델의 내부 지식 사용을 허용하는 이상
  // 빼면 안 된다. 비어 있는 값은 줄 자체를 넣지 않는다. 라벨만 있고 값이 없는 줄은 모델에게 정보가
  // 아니라 잡음이다.
  static void appendBookInfo(StringBuilder prompt, Book book) {
    prompt.append("제목: ").append(book.getTitle()).append('\n');
    appendIfPresent(prompt, "저자", book.getAuthor());
    appendIfPresent(prompt, "출판사", book.getPublisher());
    appendIfPresent(
        prompt, "출간일", book.getPublishedDate() == null ? null : book.getPublishedDate().toString());
    appendIfPresent(prompt, "ISBN", book.getIsbn());
    appendIfPresent(prompt, "분류", book.getKdcName());
    appendIfPresent(prompt, "소개", book.getDescription());
  }

  static void appendIfPresent(StringBuilder prompt, String label, String value) {
    if (value != null && !value.isBlank()) {
      prompt.append(label).append(": ").append(value.strip()).append('\n');
    }
  }

  // 줄바꿈과 연속 공백을 한 칸으로 접는다. 길이 검증이 눈에 보이는 길이와 어긋나지 않게 하려면
  // 검증 전에 접어야 하고, 대화를 프롬프트에 넣을 때는 한 발화가 여러 줄로 보이지 않아야 한다.
  //
  // (?U)가 붙어야 유니코드 공백까지 접힌다. 자바 정규식의 기본 \s는 [ \t\n\x0B\f\r]뿐이라
  // U+2028(LINE SEPARATOR)·U+2029(PARAGRAPH SEPARATOR)·U+00A0(NBSP)를 그냥 통과시킨다.
  // 그대로 두면 참여자가 U+2028로 줄을 나눈 메시지가 JSON 문자열 안에서 여러 줄로 되살아나
  // 규칙 블록을 흉내 낼 수 있다.
  //
  // 접은 결과가 비면 null이다. 접기 전 isBlank로 판단하면 NBSP만 담긴 문자열을 걸러내지 못한다
  // — Character.isWhitespace는 non-breaking space를 공백으로 보지 않는다.
  static String normalize(String text) {
    if (text == null) {
      return null;
    }
    String normalized = text.replaceAll("(?U)\\s+", " ").strip();
    return normalized.isEmpty() ? null : normalized;
  }
}
