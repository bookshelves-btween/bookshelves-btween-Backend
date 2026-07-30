package com.bookshelves.domain.ai.client;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.ObjectMapper;

// Gemini 응답 스텁. 클라이언트 테스트가 공유한다.
//
// 응답 껍질을 각 테스트가 따로 들고 있으면 응답 형식이 바뀔 때 고칠 자리가 흩어진다.
// 요청 검증은 여기서 하지 않는다. 절대 URL과 API 키 헤더는 호출 규약이라 GeminiClientTest가 맡고,
// 용도별 테스트는 프롬프트와 검증만 본다. 모두가 같은 것을 확인하면 어디가 진짜 검증인지 흐려진다.
final class GeminiTestResponses {

  private GeminiTestResponses() {}

  static String wrap(String modelText) {
    return """
        {"candidates":[{"content":{"parts":[{"text":%s}]}}]}
        """
        .formatted(new ObjectMapper().valueToTree(modelText).toString());
  }

  static void expectPost(MockRestServiceServer mockServer, String modelText) {
    mockServer
        .expect(method(HttpMethod.POST))
        .andRespond(withSuccess(wrap(modelText), MediaType.APPLICATION_JSON));
  }
}
