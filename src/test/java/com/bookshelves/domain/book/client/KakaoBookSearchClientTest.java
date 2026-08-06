package com.bookshelves.domain.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

class KakaoBookSearchClientTest {

  private static final String BOOK_SEARCH_URI = "https://dapi.kakao.com/v3/search/book";

  private MockRestServiceServer mockServer;
  private KakaoBookSearchClient kakaoBookSearchClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    kakaoBookSearchClient =
        new KakaoBookSearchClient(
            restClientBuilder.baseUrl(BOOK_SEARCH_URI).build(), "test-rest-api-key");
  }

  @Test
  void searchCallsKakaoWithoutTargetAndMapsResponse() {
    mockServer
        .expect(requestTo(startsWith(BOOK_SEARCH_URI)))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
        .andExpect(
            queryParam("query", UriUtils.encodeQueryParam("미움받을 용기", StandardCharsets.UTF_8)))
        .andExpect(queryParam("page", "1"))
        .andExpect(queryParam("size", "15"))
        .andRespond(
            withSuccess(
                """
                {
                  "meta": { "is_end": false },
                  "documents": [{
                    "isbn": "8996991341 9788996991342",
                    "title": "미움받을 용기",
                    "authors": ["기시미 이치로", "고가 후미타케"],
                    "publisher": "인플루엔셜",
                    "datetime": "2014-11-17T00:00:00.000+09:00",
                    "contents": "도서 소개",
                    "thumbnail": "https://example.com/cover.jpg"
                  }]
                }
                """,
                MediaType.APPLICATION_JSON));

    KakaoBookSearchResult result = kakaoBookSearchClient.search("미움받을 용기", 1, 15);

    assertThat(result.isEnd()).isFalse();
    assertThat(result.books()).hasSize(1);
    assertThat(result.books().getFirst().title()).isEqualTo("미움받을 용기");
    assertThat(result.books().getFirst().authors()).containsExactly("기시미 이치로", "고가 후미타케");
    mockServer.verify();
  }

  @Test
  void searchConvertsKakaoFailureToBookErrorCode() {
    mockServer
        .expect(requestTo(startsWith(BOOK_SEARCH_URI)))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    assertThatThrownBy(() -> kakaoBookSearchClient.search("책", 1, 15))
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.EXTERNAL_BOOK_API_FAILED));
  }

  @Test
  void searchEncodesBracesAsQueryParameterValue() {
    mockServer
        .expect(requestTo(BOOK_SEARCH_URI + "?query=%7Btest%7D&page=1&size=15"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {
                  "meta": { "is_end": true },
                  "documents": []
                }
                """,
                MediaType.APPLICATION_JSON));

    KakaoBookSearchResult result = kakaoBookSearchClient.search("{test}", 1, 15);

    assertThat(result.books()).isEmpty();
    assertThat(result.isEnd()).isTrue();
    mockServer.verify();
  }
}
