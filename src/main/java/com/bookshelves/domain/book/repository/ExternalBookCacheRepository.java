package com.bookshelves.domain.book.repository;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExternalBookCacheRepository {

  private static final String SEARCH_KEY_PREFIX = "book:search:v1:";
  private static final String DETAIL_KEY_PREFIX = "book:detail:v1:";
  private static final Duration SEARCH_TTL = Duration.ofMinutes(5);
  private static final Duration DETAIL_TTL = Duration.ofDays(1);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public Optional<KakaoBookSearchResult> findSearch(String normalizedQuery, int page, int size) {
    return read(searchKey(normalizedQuery, page, size), KakaoBookSearchResult.class);
  }

  public void saveSearch(
      String normalizedQuery, int page, int size, KakaoBookSearchResult searchResult) {
    write(searchKey(normalizedQuery, page, size), searchResult, SEARCH_TTL);
  }

  public Optional<CachedBookDetail> findDetail(String canonicalIsbn) {
    return read(detailKey(canonicalIsbn), CachedBookDetail.class);
  }

  public void saveDetail(String canonicalIsbn, CachedBookDetail bookDetail) {
    write(detailKey(canonicalIsbn), bookDetail, DETAIL_TTL);
  }

  private <T> Optional<T> read(String key, Class<T> type) {
    try {
      String cachedValue = stringRedisTemplate.opsForValue().get(key);
      if (cachedValue == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(cachedValue, type));
    } catch (RuntimeException exception) {
      log.warn("외부 도서 캐시 조회에 실패해 외부 API 조회를 진행합니다. key={}", key, exception);
      return Optional.empty();
    }
  }

  private void write(String key, Object value, Duration ttl) {
    try {
      stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (RuntimeException exception) {
      log.warn("외부 도서 캐시 저장에 실패했습니다. key={}", key, exception);
    }
  }

  private String searchKey(String normalizedQuery, int page, int size) {
    return SEARCH_KEY_PREFIX + sha256(normalizedQuery) + ":" + page + ":" + size;
  }

  private String detailKey(String canonicalIsbn) {
    return DETAIL_KEY_PREFIX + canonicalIsbn;
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
    }
  }

  public record CachedBookDetail(KakaoBookItem item, String canonicalIsbn, KdcInfo kdcInfo) {}
}
