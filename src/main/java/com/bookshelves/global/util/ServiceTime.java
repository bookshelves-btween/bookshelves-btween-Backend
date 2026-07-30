package com.bookshelves.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 서비스 기준 시간대.
//
// 이 값이 여기저기 복제되면 어느 한 곳만 UTC로 남아도 하루 경계에서만 어긋나 재현이 어렵다.
// 서버가 어느 시간대에서 돌든 사용자가 보는 날짜는 한국 기준이어야 한다.
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
