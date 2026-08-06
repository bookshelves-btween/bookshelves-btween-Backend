package com.bookshelves.domain.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class Data4LibraryBookDetailClientTest {

  private static final String BASE_URL = "https://data4library.kr/api";
  private static final String ISBN = "9788936434595";

  private MockRestServiceServer mockServer;
  private Data4LibraryBookDetailClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    client =
        new Data4LibraryBookDetailClient(restClientBuilder.baseUrl(BASE_URL).build(), "test-key");
  }

  @Test
  void mapsFirstThreeDigitsOfValidKdcCode() {
    respondWithClassNumber("813.6");

    KdcInfo result = client.findKdcByIsbn(ISBN);

    assertThat(result.code()).isEqualTo("813");
    assertThat(result.name()).isEqualTo("문학");
    mockServer.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {"8", "81"})
  void returnsNullKdcWhenClassNumberHasFewerThanThreeDigits(String classNumber) {
    respondWithClassNumber(classNumber);

    KdcInfo result = client.findKdcByIsbn(ISBN);

    assertThat(result.code()).isNull();
    assertThat(result.name()).isNull();
    mockServer.verify();
  }

  @Test
  void returnsNullKdcWhenResponseHasNoBookDetail() {
    mockServer
        .expect(requestTo(startsWith(BASE_URL + "/srchDtlList")))
        .andRespond(withSuccess("{\"response\":{\"detail\":[]}}", MediaType.APPLICATION_JSON));

    KdcInfo result = client.findKdcByIsbn(ISBN);

    assertThat(result).isEqualTo(KdcInfo.unavailable());
    mockServer.verify();
  }

  @Test
  void returnsNullKdcWhenData4LibraryRequestFails() {
    mockServer
        .expect(requestTo(startsWith(BASE_URL + "/srchDtlList")))
        .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    KdcInfo result = client.findKdcByIsbn(ISBN);

    assertThat(result).isEqualTo(KdcInfo.unavailable());
    mockServer.verify();
  }

  @Test
  void returnsNullKdcWhenAuthKeyIsMissing() {
    Data4LibraryBookDetailClient clientWithoutAuthKey =
        new Data4LibraryBookDetailClient(RestClient.builder().baseUrl(BASE_URL).build(), " ");

    KdcInfo result = clientWithoutAuthKey.findKdcByIsbn(ISBN);

    assertThat(result).isEqualTo(KdcInfo.unavailable());
  }

  private void respondWithClassNumber(String classNumber) {
    mockServer
        .expect(requestTo(startsWith(BASE_URL + "/srchDtlList")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {
                  "response": {
                    "detail": [{
                      "book": { "class_no": "%s" }
                    }]
                  }
                }
                """
                    .formatted(classNumber),
                MediaType.APPLICATION_JSON));
  }
}
