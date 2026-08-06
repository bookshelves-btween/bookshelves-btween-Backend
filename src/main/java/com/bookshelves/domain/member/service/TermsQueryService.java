package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.converter.TermsConverter;
import com.bookshelves.domain.member.dto.response.TermsResponse;
import com.bookshelves.domain.member.repository.TermsRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TermsQueryService {

  private final TermsRepository termsRepository;

  public TermsQueryService(TermsRepository termsRepository) {
    this.termsRepository = termsRepository;
  }

  public List<TermsResponse> getTermsList() {
    return termsRepository.findAll().stream().map(TermsConverter::toTermsResponse).toList();
  }
}
