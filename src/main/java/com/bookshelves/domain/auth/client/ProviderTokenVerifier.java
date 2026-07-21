package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.member.enums.Provider;

public interface ProviderTokenVerifier {

  Provider getProvider();

  ProviderUserInfo verify(String providerToken);
}
