package com.bookshelves.global.apiPayload;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

  @Getter(AccessLevel.NONE)
  private final boolean isSuccess;

  private final String code;
  private final String message;
  private final T result;

  // Lombok이 boolean 필드 isSuccess에 생성하는 게터명은 isSuccess()이고,
  // Jackson이 is 접두사를 벗겨 JSON 키가 "success"가 되므로 명시적으로 고정한다.
  @JsonProperty("isSuccess")
  public boolean isSuccess() {
    return isSuccess;
  }

  // 성공
  public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
    return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
  }

  // 실패
  public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
    return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
  }
}
