package com.bookshelves.global.config;

import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

// 모든 자체 에러 메시지는 한국어로 고정돼 있는데, @NotBlank 등 라이브러리 기본 검증
// 메시지만 클라이언트 Accept-Language 헤더에 따라 영어 등으로 갈리면 일관성이 깨진다.
// spring.mvc.locale-resolver=fixed 같은 프로퍼티만으로는 실제로 반영되지 않아 빈을
// 직접 등록해 강제한다. 요청 헤더와 무관하게 항상 한국어로 고정한다.
@Configuration
public class LocaleConfig {

  @Bean
  public LocaleResolver localeResolver() {
    return new FixedLocaleResolver(Locale.KOREA);
  }
}
