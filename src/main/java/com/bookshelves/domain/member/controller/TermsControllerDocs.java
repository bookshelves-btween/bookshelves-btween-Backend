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
                    "content": "## 제1조 목적\\n본 약관은 책장사이가 제공하는 독서 기록, 내 서재, 독서 모임, 익명 채팅 및 AI 대화 요약 서비스의 이용 조건과 절차를 규정합니다.\\n\\n## 제2조 회원가입 및 계정\\n1. 이용자는 카카오 또는 구글 소셜 로그인을 통해 가입할 수 있습니다.\\n2. 이용자는 타인의 계정을 이용하거나 자신의 계정을 제3자에게 양도·공유해서는 안 됩니다.\\n3. 소셜 로그인 제공자의 정책 또는 계정 상태에 따라 로그인이 제한될 수 있습니다.\\n\\n## 제3조 서비스 제공\\n책장사이는 다음 서비스를 제공합니다.\\n1. 도서 검색 및 독서 기록 관리\\n2. 내 서재 및 독서 통계\\n3. 독서 모임 생성·검색·참여\\n4. 익명 실시간 채팅\\n5. AI 발제문 및 대화 요약\\n6. 모임 및 서비스 관련 알림\\n\\n## 제4조 이용자의 의무\\n이용자는 다음 행위를 해서는 안 됩니다.\\n1. 타인을 사칭하거나 허위 정보를 등록하는 행위\\n2. 욕설, 혐오, 음란, 불법 또는 타인의 권리를 침해하는 내용을 작성하는 행위\\n3. 채팅 도배, 광고 또는 서비스 운영을 방해하는 행위\\n4. 다른 이용자의 개인정보를 수집하거나 외부에 공개하는 행위\\n5. 비정상적인 방법으로 서비스에 접근하거나 기술적 보호조치를 우회하는 행위\\n\\n## 제5조 신고 및 이용 제한\\n운영정책을 위반한 이용자는 신고될 수 있습니다. 위반 정도에 따라 콘텐츠 삭제, 채팅 참여 제한, 일시 정지 또는 영구 이용 제한 조치가 적용될 수 있습니다.\\n\\n## 제6조 이용자 콘텐츠\\n이용자가 작성한 독서 기록과 채팅 내용에 대한 권리는 해당 이용자에게 있습니다. 다만, 서비스 제공·운영 및 AI 대화 요약 생성에 필요한 범위에서 해당 콘텐츠가 저장·처리될 수 있습니다.\\n\\n## 제7조 모임 운영\\n1. 모임은 정해진 모집 기간, 정원 및 진행시간에 따라 운영됩니다.\\n2. 모집 마감 시 최소 참여 인원을 충족하지 못한 모임은 자동 취소될 수 있습니다.\\n3. 이용자는 참여한 모임의 운영 규칙과 채팅 운영정책을 준수해야 합니다.\\n4. 정당한 사유 없이 모임에 참여하지 않은 경우 노쇼 기록이 생성될 수 있습니다.\\n\\n## 제8조 회원탈퇴\\n1. 이용자는 계정 설정에서 회원탈퇴를 신청할 수 있습니다.\\n2. 탈퇴 신청 후 30일 동안 탈퇴 유예 상태가 유지됩니다.\\n3. 유예기간 내 다시 로그인하면 기존 계정을 복구할 수 있습니다.\\n4. 30일이 지나면 회원탈퇴가 최종 완료되며, 관계 법령에 따른 보존 대상 정보를 제외한 개인정보가 파기됩니다.\\n\\n## 제9조 서비스 변경 및 중단\\n서비스 점검, 장애 또는 운영상 필요한 사유가 있는 경우 서비스의 일부 또는 전부가 일시적으로 제한될 수 있습니다.\\n\\n## 제10조 약관 변경\\n약관이 변경되는 경우 적용일과 주요 변경 내용을 서비스 내 공지 등을 통해 안내합니다.",
                    "type": "SERVICE",
                    "version": "1.0.0",
                    "isRequired": true
                  },
                  {
                    "id": 2,
                    "title": "개인정보 수집 및 이용 동의",
                    "content": "책장사이는 서비스 제공에 필요한 최소한의 개인정보를 수집·이용합니다.\\n\\n## 1. 수집·이용 목적\\n- 카카오·구글 소셜 로그인 및 회원 식별\\n- 회원가입, 계정 관리 및 계정 복구\\n- 도서 검색, 독서 기록 및 내 서재 기능 제공\\n- 독서 모임 생성·검색·참여 기능 제공\\n- 익명 실시간 채팅 및 AI 대화 요약 제공\\n- 서비스 알림 및 신고·부정 이용 방지\\n\\n## 2. 수집 항목\\n- 소셜 로그인 제공자\\n- 소셜 계정 고유 식별값\\n- 이메일 주소: 소셜 로그인 제공자로부터 실제로 제공받는 경우\\n- 서비스 내 랜덤 닉네임\\n- 관심 장르\\n\\n> 서비스 이용 과정에서 아래 정보가 추가로 생성·저장될 수 있습니다\\n\\n## 3. 보유 및 이용 기간\\n회원탈퇴 신청일로부터 30일 동안 계정을 탈퇴 유예 상태로 보관합니다. 유예기간 내 다시 로그인할 경우 기존 계정을 복구할 수 있으며, 30일이 지나면 회원탈퇴가 최종 완료됩니다. 탈퇴 완료 후 개인정보는 관계 법령에 따라 보관할 의무가 있는 정보를 제외하고 파기합니다.\\n\\n## 4. 동의 거부 권리 및 불이익\\n이용자는 개인정보 수집·이용 동의를 거부할 수 있습니다. 다만, 필수 개인정보 수집·이용에 동의하지 않을 경우 회원가입 및 책장사이 서비스 이용이 제한됩니다.",
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
