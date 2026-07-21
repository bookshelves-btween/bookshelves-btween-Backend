package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.exception.ProjectException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProviderTokenVerifierResolver {

  private final Map<Provider, ProviderTokenVerifier> verifiersByProvider;

  public ProviderTokenVerifierResolver(List<ProviderTokenVerifier> verifiers) {
    this.verifiersByProvider =
        verifiers.stream()
            .collect(Collectors.toMap(ProviderTokenVerifier::getProvider, Function.identity()));
  }

  public ProviderTokenVerifier resolve(Provider provider) {
    ProviderTokenVerifier verifier = verifiersByProvider.get(provider);

    if (verifier == null) {
      throw new ProjectException(AuthErrorCode.UNSUPPORTED_PROVIDER);
    }

    return verifier;
  }
}
