package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.book.entity.Book;

// Gemini 클라이언트에서 공통으로 사용하는 프롬프트 유틸리티.
final class Prompts {

  private Prompts() {}

  // 출간일과 ISBN을 포함해 동명 도서와 개정판을 구분하고, 빈 항목은 제외한다.
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

  // 유니코드 공백까지 한 칸으로 합치며, 정규화 후 빈 문자열은 null로 처리한다.
  static String normalize(String text) {
    if (text == null) {
      return null;
    }
    String normalized = text.replaceAll("(?U)\\s+", " ").strip();
    return normalized.isEmpty() ? null : normalized;
  }
}
