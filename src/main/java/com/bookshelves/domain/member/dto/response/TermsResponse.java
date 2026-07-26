package com.bookshelves.domain.member.dto.response;

import com.bookshelves.domain.member.enums.TermsType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsResponse {

  private Long id;
  private String title;
  private String content;
  private TermsType type;
  private String version;
  private Boolean isRequired;
}
