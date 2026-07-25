package com.bookshelves.domain.book.client;

import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoBookSearchClient {

  private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

  private final RestClient restClient;
  private final String restApiKey;

  @Autowired
  public KakaoBookSearchClient(
      RestClient.Builder restClientBuilder,
      @Value("${external.kakao.rest-api-key}") String restApiKey,
      @Value("${external.kakao.book-search-uri}") String bookSearchUri) {
    this(buildRestClient(restClientBuilder, bookSearchUri), restApiKey);
  }

  KakaoBookSearchClient(RestClient restClient, String restApiKey) {
    this.restClient = restClient;
    this.restApiKey = restApiKey;
  }

  private static RestClient buildRestClient(
      RestClient.Builder restClientBuilder, String bookSearchUri) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    return restClientBuilder.baseUrl(bookSearchUri).requestFactory(requestFactory).build();
  }

  public KakaoBookSearchResult search(String query, int page, int size) {
    return search(query, page, size, null);
  }

  public KakaoBookSearchResult searchByIsbn(String isbn) {
    return search(isbn, 1, 1, "isbn");
  }

  private KakaoBookSearchResult search(String query, int page, int size, String target) {
    if (restApiKey == null || restApiKey.isBlank()) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_API_FAILED);
    }

    try {
      KakaoBookSearchResponse response =
          restClient
              .get()
              .uri(
                  uriBuilder -> {
                    uriBuilder
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", size);
                    if (target != null) {
                      uriBuilder.queryParam("target", target);
                    }
                    return uriBuilder.build();
                  })
              .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + restApiKey)
              .retrieve()
              .body(KakaoBookSearchResponse.class);

      if (response == null || response.meta() == null || response.documents() == null) {
        throw new BookException(BookErrorCode.EXTERNAL_BOOK_API_FAILED);
      }

      List<KakaoBookItem> books =
          response.documents().stream()
              .map(
                  document ->
                      new KakaoBookItem(
                          document.isbn(),
                          document.title(),
                          document.authors() == null ? List.of() : document.authors(),
                          document.publisher(),
                          document.datetime(),
                          document.contents(),
                          document.thumbnail()))
              .toList();
      return new KakaoBookSearchResult(books, response.meta().isEnd());
    } catch (BookException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_API_FAILED);
    }
  }

  public record KakaoBookSearchResult(List<KakaoBookItem> books, boolean isEnd) {}

  public record KakaoBookItem(
      String isbn,
      String title,
      List<String> authors,
      String publisher,
      String datetime,
      String contents,
      String thumbnail) {}

  private record KakaoBookSearchResponse(Meta meta, List<Document> documents) {}

  private record Meta(@JsonProperty("is_end") boolean isEnd) {}

  private record Document(
      String isbn,
      String title,
      List<String> authors,
      String publisher,
      String datetime,
      String contents,
      String thumbnail) {}
}
