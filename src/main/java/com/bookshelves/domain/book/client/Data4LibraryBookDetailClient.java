package com.bookshelves.domain.book.client;

import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class Data4LibraryBookDetailClient {

  private static final Map<Character, String> KDC_NAMES =
      Map.of(
          '0', "총류",
          '1', "철학",
          '2', "종교",
          '3', "사회과학",
          '4', "자연과학",
          '5', "기술과학",
          '6', "예술",
          '7', "언어",
          '8', "문학",
          '9', "역사");

  private final RestClient restClient;
  private final String authKey;

  @Autowired
  public Data4LibraryBookDetailClient(
      RestClient.Builder restClientBuilder,
      @Value("${external.data4library.auth-key}") String authKey,
      @Value("${external.data4library.base-url}") String baseUrl) {
    this(buildRestClient(restClientBuilder, baseUrl), authKey);
  }

  Data4LibraryBookDetailClient(RestClient restClient, String authKey) {
    this.restClient = restClient;
    this.authKey = authKey;
  }

  public KdcInfo findKdcByIsbn(String isbn) {
    if (authKey == null || authKey.isBlank()) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_API_FAILED);
    }

    try {
      JsonNode response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/srchDtlList")
                          .queryParam("authKey", authKey)
                          .queryParam("isbn13", isbn)
                          .queryParam("format", "json")
                          .build())
              .retrieve()
              .body(JsonNode.class);

      String classNumber = extractClassNumber(response);
      if (classNumber == null) {
        return new KdcInfo(null, "미분류");
      }

      String kdcCode = normalizeKdcCode(classNumber);
      if (kdcCode.isBlank()) {
        return new KdcInfo(null, "미분류");
      }
      String kdcName = KDC_NAMES.getOrDefault(kdcCode.charAt(0), "미분류");
      return new KdcInfo(kdcCode, kdcName);
    } catch (RestClientException exception) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_API_FAILED);
    }
  }

  private static RestClient buildRestClient(RestClient.Builder restClientBuilder, String baseUrl) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    return restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  private String extractClassNumber(JsonNode response) {
    if (response == null) {
      return null;
    }

    JsonNode detail = response.path("response").path("detail");
    if (!detail.isArray() || detail.isEmpty()) {
      return null;
    }

    JsonNode classNumber = detail.get(0).path("book").path("class_no");
    return classNumber.isTextual() && !classNumber.asText().isBlank() ? classNumber.asText() : null;
  }

  private String normalizeKdcCode(String classNumber) {
    String digits = classNumber.replaceAll("\\D", "");
    return digits.length() >= 3 ? digits.substring(0, 3) : digits;
  }

  public record KdcInfo(String code, String name) {}
}
