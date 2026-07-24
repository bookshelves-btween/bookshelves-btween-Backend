package com.bookshelves.domain.member.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberWithdrawResponse {

  private OffsetDateTime scheduledDeletionAt;
}
