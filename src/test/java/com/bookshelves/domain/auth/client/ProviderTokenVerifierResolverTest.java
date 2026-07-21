package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderTokenVerifierResolverTest {

  @Test
  void resolveReturnsMatchingVerifier() {
    ProviderTokenVerifier kakaoVerifier = mock(ProviderTokenVerifier.class);
    when(kakaoVerifier.getProvider()).thenReturn(Provider.KAKAO);
    ProviderTokenVerifierResolver resolver =
        new ProviderTokenVerifierResolver(List.of(kakaoVerifier));

    assertThat(resolver.resolve(Provider.KAKAO)).isSameAs(kakaoVerifier);
  }

  @Test
  void resolveThrowsForUnsupportedProvider() {
    ProviderTokenVerifierResolver resolver = new ProviderTokenVerifierResolver(List.of());

    assertThatThrownBy(() -> resolver.resolve(Provider.GOOGLE))
        .isInstanceOf(ProjectException.class);
  }
}
