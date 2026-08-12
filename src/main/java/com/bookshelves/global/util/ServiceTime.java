package com.bookshelves.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 서버 시간대와 관계없이 서비스 날짜와 시간을 한국 기준으로 제공한다.
public final class ServiceTime {

  public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private ServiceTime() {}

  public static LocalDate today() {
    return LocalDate.now(ZONE);
  }

  public static LocalDateTime now() {
    return LocalDateTime.now(ZONE);
  }
}
