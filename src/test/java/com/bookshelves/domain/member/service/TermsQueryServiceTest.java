package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.dto.response.TermsResponse;
import com.bookshelves.domain.member.entity.Terms;
import com.bookshelves.domain.member.enums.TermsType;
import com.bookshelves.domain.member.repository.TermsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermsQueryServiceTest {

  private final TermsRepository termsRepository = mock(TermsRepository.class);
  private final TermsQueryService termsQueryService = new TermsQueryService(termsRepository);

  @Test
  void getTermsListReturnsOnlyActiveTerms() {
    Terms serviceTerms = mock(Terms.class);
    when(serviceTerms.getId()).thenReturn(1L);
    when(serviceTerms.getTitle()).thenReturn("서비스 이용약관");
    when(serviceTerms.getContent()).thenReturn("제1조 (목적) ...");
    when(serviceTerms.getType()).thenReturn(TermsType.SERVICE);
    when(serviceTerms.getVersion()).thenReturn("1.0.0");
    when(serviceTerms.getIsRequired()).thenReturn(true);
    // 리포지토리 메서드명(findByIsActiveTrue) 자체가 비활성(구버전) row를 걸러내는 지점이라,
    // 여기선 리포지토리가 활성 row만 돌려준다고 가정하고 서비스가 그걸 그대로 매핑하는지 검증한다.
    when(termsRepository.findByIsActiveTrue()).thenReturn(List.of(serviceTerms));

    List<TermsResponse> response = termsQueryService.getTermsList();

    assertThat(response).hasSize(1);
    assertThat(response.get(0).getId()).isEqualTo(1L);
    assertThat(response.get(0).getTitle()).isEqualTo("서비스 이용약관");
    assertThat(response.get(0).getContent()).isEqualTo("제1조 (목적) ...");
    assertThat(response.get(0).getType()).isEqualTo(TermsType.SERVICE);
    assertThat(response.get(0).getVersion()).isEqualTo("1.0.0");
    assertThat(response.get(0).getIsRequired()).isTrue();
  }

  @Test
  void getTermsListReturnsEmptyListWhenNoActiveTerms() {
    when(termsRepository.findByIsActiveTrue()).thenReturn(List.of());

    List<TermsResponse> response = termsQueryService.getTermsList();

    assertThat(response).isEmpty();
  }
}
