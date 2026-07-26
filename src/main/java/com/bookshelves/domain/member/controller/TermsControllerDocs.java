package com.bookshelves.domain.member.controller;

import com.bookshelves.domain.member.dto.response.TermsResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "회원", description = "회원·온보딩 API")
public interface TermsControllerDocs {

  @Operation(summary = "약관 목록 조회", description = "온보딩에 필요한 전체 약관 목록을 조회한다. 인증이 필요 없다.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "TERMS200_1",
                "message": "약관 목록 조회에 성공하였습니다.",
                "result": [
                  {
                    "id": 1,
                    "title": "서비스 이용약관",
                    "content": "## 제1조 목적\\n본 약관은 ...",
                    "type": "SERVICE",
                    "version": "1.0.0",
                    "isRequired": true
                  },
                  {
                    "id": 2,
                    "title": "개인정보 수집 및 이용 동의",
                    "content": "## 1. 수집·이용 목적\\n...",
                    "type": "PRIVACY",
                    "version": "1.0.0",
                    "isRequired": true
                  }
                ]
              }
              """)))
  })
  @GetMapping("/api/v1/onboarding/terms")
  ResponseEntity<ApiResponse<List<TermsResponse>>> getTermsList();
}
