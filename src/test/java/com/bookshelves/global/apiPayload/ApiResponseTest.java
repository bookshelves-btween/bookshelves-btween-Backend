package com.bookshelves.global.apiPayload;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.global.apiPayload.code.GeneralSuccessCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void serializesIsSuccessKeyInsteadOfSuccess() {
    ApiResponse<String> response = ApiResponse.onSuccess(GeneralSuccessCode.COMMON_OK, "result");

    String json = jsonMapper.writeValueAsString(response);

    assertThat(json).contains("\"isSuccess\":true");
    assertThat(json).doesNotContain("\"success\":");
  }
}
