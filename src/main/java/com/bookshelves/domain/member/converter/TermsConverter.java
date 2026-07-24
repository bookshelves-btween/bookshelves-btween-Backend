package com.bookshelves.domain.member.converter;

import com.bookshelves.domain.member.dto.response.TermsResponse;
import com.bookshelves.domain.member.entity.Terms;

public class TermsConverter {

  private TermsConverter() {}

  public static TermsResponse toTermsResponse(Terms terms) {
    return TermsResponse.builder()
        .id(terms.getId())
        .title(terms.getTitle())
        .content(terms.getContent())
        .type(terms.getType())
        .version(terms.getVersion())
        .isRequired(terms.getIsRequired())
        .build();
  }
}
