package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.repository.ExternalBookCacheRepository.CachedBookDetail;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ExternalBookCacheRepositoryTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;
  @Mock private ObjectMapper objectMapper;
  @InjectMocks private ExternalBookCacheRepository externalBookCacheRepository;

  @BeforeEach
  void setUpValueOperations() {
    given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
  }

  @Test
  void searchUsesHashedVersionedKeyAndFiveMinuteTtl() {
    KakaoBookSearchResult result = new KakaoBookSearchResult(List.of(), true);
    given(objectMapper.writeValueAsString(result)).willReturn("json");

    externalBookCacheRepository.saveSearch("혼모노", 2, 15, result);

    verify(valueOperations)
        .set(
            org.mockito.ArgumentMatchers.matches("book:search:v1:[0-9a-f]{64}:2:15"),
            eq("json"),
            eq(Duration.ofMinutes(5)));
  }

  @Test
  void detailUsesCanonicalIsbnKeyAndOneDayTtl() {
    CachedBookDetail detail = cachedBookDetail();
    given(objectMapper.writeValueAsString(detail)).willReturn("json");

    externalBookCacheRepository.saveDetail("9788936434595", detail);

    verify(valueOperations).set("book:detail:v1:9788936434595", "json", Duration.ofDays(1));
  }

  @Test
  void returnsCachedDetailWhenDeserializationSucceeds() {
    CachedBookDetail detail = cachedBookDetail();
    given(valueOperations.get("book:detail:v1:9788936434595")).willReturn("json");
    given(objectMapper.readValue("json", CachedBookDetail.class)).willReturn(detail);

    Optional<CachedBookDetail> result = externalBookCacheRepository.findDetail("9788936434595");

    assertThat(result).containsSame(detail);
  }

  @Test
  void searchWithNullBooksFallsBackToCacheMiss() {
    KakaoBookSearchResult incompleteResult = new KakaoBookSearchResult(null, true);
    given(valueOperations.get(anyString())).willReturn("json");
    given(objectMapper.readValue("json", KakaoBookSearchResult.class)).willReturn(incompleteResult);

    assertThat(externalBookCacheRepository.findSearch("cached-query", 1, 15)).isEmpty();
  }

  @Test
  void detailWithNullItemFallsBackToCacheMiss() {
    CachedBookDetail incompleteDetail =
        new CachedBookDetail(null, "9788936434595", KdcInfo.unavailable());
    given(valueOperations.get(anyString())).willReturn("json");
    given(objectMapper.readValue("json", CachedBookDetail.class)).willReturn(incompleteDetail);

    assertThat(externalBookCacheRepository.findDetail("9788936434595")).isEmpty();
  }

  @Test
  void detailWithNullKdcInfoFallsBackToCacheMiss() {
    CachedBookDetail validDetail = cachedBookDetail();
    CachedBookDetail incompleteDetail =
        new CachedBookDetail(validDetail.item(), validDetail.canonicalIsbn(), null);
    given(valueOperations.get(anyString())).willReturn("json");
    given(objectMapper.readValue("json", CachedBookDetail.class)).willReturn(incompleteDetail);

    assertThat(externalBookCacheRepository.findDetail("9788936434595")).isEmpty();
  }

  @Test
  void detailWithMissingCanonicalIsbnFallsBackToCacheMiss() {
    CachedBookDetail validDetail = cachedBookDetail();
    CachedBookDetail incompleteDetail =
        new CachedBookDetail(validDetail.item(), null, validDetail.kdcInfo());
    given(valueOperations.get(anyString())).willReturn("json");
    given(objectMapper.readValue("json", CachedBookDetail.class)).willReturn(incompleteDetail);

    assertThat(externalBookCacheRepository.findDetail("9788936434595")).isEmpty();
  }

  @Test
  void detailWithUnavailableKdcInfoRemainsValidCacheHit() {
    CachedBookDetail validDetail = cachedBookDetail();
    CachedBookDetail detail =
        new CachedBookDetail(
            validDetail.item(), validDetail.canonicalIsbn(), KdcInfo.unavailable());
    given(valueOperations.get(anyString())).willReturn("json");
    given(objectMapper.readValue("json", CachedBookDetail.class)).willReturn(detail);

    assertThat(externalBookCacheRepository.findDetail("9788936434595")).containsSame(detail);
  }

  @Test
  void redisReadFailureFallsBackToCacheMiss() {
    given(valueOperations.get(anyString())).willThrow(new RuntimeException("redis unavailable"));

    assertThat(externalBookCacheRepository.findDetail("9788936434595")).isEmpty();
  }

  @Test
  void redisWriteFailureDoesNotEscape() {
    CachedBookDetail detail = cachedBookDetail();
    given(objectMapper.writeValueAsString(detail)).willReturn("json");
    doThrow(new RuntimeException("redis unavailable"))
        .when(valueOperations)
        .set(anyString(), eq("json"), eq(Duration.ofDays(1)));

    externalBookCacheRepository.saveDetail("9788936434595", detail);
  }

  private CachedBookDetail cachedBookDetail() {
    KakaoBookItem item =
        new KakaoBookItem("9788936434595", "아몬드", List.of("손원평"), "창비", null, null, null);
    return new CachedBookDetail(item, "9788936434595", new KdcInfo("813", "문학"));
  }
}
