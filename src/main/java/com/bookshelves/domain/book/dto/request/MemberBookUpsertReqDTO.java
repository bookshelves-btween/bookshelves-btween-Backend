package com.bookshelves.domain.book.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "내 서재 독서 기록 저장·수정 요청")
public record MemberBookUpsertReqDTO(
    @Schema(description = "독서 진행률", example = "50", minimum = "0", maximum = "100")
        @NotNull(message = "독서 진행률은 필수입니다.")
        @Min(value = 0, message = "독서 진행률은 0 이상이어야 합니다.")
        @Max(value = 100, message = "독서 진행률은 100 이하여야 합니다.")
        Integer progress,
    @Schema(description = "평점", example = "4.5", minimum = "0", maximum = "5", nullable = true)
        @DecimalMin(value = "0.0", message = "평점은 0 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5 이하여야 합니다.")
        @Digits(integer = 1, fraction = 1, message = "평점은 소수점 첫째 자리까지 입력할 수 있습니다.")
        BigDecimal rating,
    @Schema(description = "한줄평", example = "생각보다 좋았다.", nullable = true)
        @Size(max = 200, message = "한줄평은 200자 이하여야 합니다.")
        String memo) {

  @AssertTrue(message = "평점은 0.5 단위로 입력해야 합니다.")
  public boolean isRatingInHalfPointIncrements() {
    return rating == null
        || rating.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) == 0;
  }
}
