package com.bookshelves.global.security;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.global.exception.ProjectException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacade {

  public Long getCurrentMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
      throw new ProjectException(AuthErrorCode.INVALID_ACCESS_TOKEN);
    }

    return principal.memberId();
  }
}
