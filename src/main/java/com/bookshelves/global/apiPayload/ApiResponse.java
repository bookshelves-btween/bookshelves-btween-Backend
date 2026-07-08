package com.bookshelves.global.apiPayload;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

  private final boolean isSuccess;
  private final String code;
  private final String message;
  private final T result;

  // 성공
  public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
    return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
  }

  // 실패
  public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
    return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
  }
}
