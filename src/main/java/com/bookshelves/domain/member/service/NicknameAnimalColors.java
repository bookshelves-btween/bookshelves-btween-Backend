package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import java.util.Map;

public class NicknameAnimalColors {

  private NicknameAnimalColors() {}

  public static final Map<String, ProfileBackgroundColor> DEFAULT_COLORS =
      Map.ofEntries(
          Map.entry("곰", ProfileBackgroundColor.BROWN),
          Map.entry("다람쥐", ProfileBackgroundColor.BROWN),
          Map.entry("고슴도치", ProfileBackgroundColor.BROWN),
          Map.entry("나무늘보", ProfileBackgroundColor.BROWN),
          Map.entry("여우", ProfileBackgroundColor.BROWN),
          Map.entry("토끼", ProfileBackgroundColor.RED),
          Map.entry("비버", ProfileBackgroundColor.RED),
          Map.entry("수달", ProfileBackgroundColor.RED),
          Map.entry("얼룩말", ProfileBackgroundColor.RED),
          Map.entry("올빼미", ProfileBackgroundColor.PURPLE),
          Map.entry("너구리", ProfileBackgroundColor.PURPLE),
          Map.entry("북극곰", ProfileBackgroundColor.PURPLE),
          Map.entry("판다", ProfileBackgroundColor.PURPLE),
          Map.entry("코알라", ProfileBackgroundColor.PURPLE),
          Map.entry("사자", ProfileBackgroundColor.YELLOW),
          Map.entry("호랑이", ProfileBackgroundColor.YELLOW),
          Map.entry("치타", ProfileBackgroundColor.YELLOW),
          Map.entry("기린", ProfileBackgroundColor.YELLOW),
          Map.entry("악어", ProfileBackgroundColor.GREEN),
          Map.entry("거북이", ProfileBackgroundColor.GREEN),
          Map.entry("개구리", ProfileBackgroundColor.GREEN),
          Map.entry("고래", ProfileBackgroundColor.BLUE),
          Map.entry("상어", ProfileBackgroundColor.BLUE),
          Map.entry("펭귄", ProfileBackgroundColor.BLUE),
          Map.entry("코끼리", ProfileBackgroundColor.BLUE));
}
