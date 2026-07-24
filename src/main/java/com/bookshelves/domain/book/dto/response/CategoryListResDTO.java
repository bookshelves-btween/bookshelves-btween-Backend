package com.bookshelves.domain.book.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "KDC 최상위 카테고리 목록 조회 결과")
public record CategoryListResDTO(List<CategoryInfo> categories) {

  @Schema(description = "KDC 최상위 카테고리")
  public record CategoryInfo(Long id, String kdcCode, String name) {}
}
