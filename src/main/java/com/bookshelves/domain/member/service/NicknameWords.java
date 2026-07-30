package com.bookshelves.domain.member.service;

import java.util.Set;

public class NicknameWords {

  private NicknameWords() {}

  public static final Set<String> NOUNS =
      Set.of(
          "책", "문장", "책장", "책갈피", "서재", "소설", "시집", "단어", "문단", "페이지", "표지", "잉크", "글자", "목차", "여백",
          "문고", "도서", "장면", "구절", "결말", "북마크", "책등", "한줄평", "동화", "고전", "이야기", "책방", "도서관");

  public static final Set<String> MODIFIERS =
      Set.of(
          "먹는", "읽는", "빌리는", "엎는", "던지는", "모으는", "여는", "덮는", "고르는", "숨기는", "줍는", "찾는", "따라가는",
          "물고 온", "기다리는", "속삭이는", "좋아하는", "기록하는", "간직하는", "훔치는", "지키는", "넘기는", "쏟는", "적는", "옮기는",
          "나르는");

  public static final Set<String> ANIMALS =
      Set.of(
          "곰", "다람쥐", "고슴도치", "나무늘보", "올빼미", "너구리", "북극곰", "판다", "코끼리", "고래", "상어", "펭귄", "악어",
          "개구리", "거북이", "토끼", "수달", "비버", "얼룩말", "사자", "호랑이", "치타", "기린", "코알라");
}
