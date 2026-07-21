package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.global.config.JwtProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600));
  private final JwtAuthenticationFilter jwtAuthenticationFilter =
      new JwtAuthenticationFilter(jwtTokenProvider);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validAccessTokenSetsMemberPrincipal() throws ServletException, IOException {
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(1L));
  }

  @Test
  void refreshTokenDoesNotSetAuthentication() throws ServletException, IOException {
    String token = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
