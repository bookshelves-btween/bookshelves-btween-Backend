package com.bookshelves.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

  private final String[] allowUris = {
    // Swagger 허용
    "/swagger-ui/**",
    "/swagger-resources/**",
    "/v3/api-docs/**",
    "/api/v1/**" // 인증 기능 구현 전까지 전체 API 접근 임시 허용

  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(requests -> requests
        .requestMatchers(allowUris).permitAll()
        .anyRequest().authenticated()
      )
      .formLogin(form -> form
        .defaultSuccessUrl("/swagger-ui/index.html", true)
        .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl("/logout")
        .logoutSuccessUrl("/login?logout")
        .permitAll()
      );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
