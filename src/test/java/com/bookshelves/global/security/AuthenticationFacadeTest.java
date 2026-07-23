package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticationFacadeTest {

  private final AuthenticationFacade authenticationFacade = new AuthenticationFacade();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentMemberIdReturnsAuthenticatedMemberId() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(1L), null, List.of()));

    assertThat(authenticationFacade.getCurrentMemberId()).isEqualTo(1L);
  }

  @Test
  void getCurrentMemberIdThrowsWhenNoAuthenticationPresent() {
    assertThatThrownBy(authenticationFacade::getCurrentMemberId)
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
  }

  @Test
  void getCurrentMemberIdThrowsWhenPrincipalIsNotMemberPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("not-a-member", null, List.of()));

    assertThatThrownBy(authenticationFacade::getCurrentMemberId)
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
  }
}
