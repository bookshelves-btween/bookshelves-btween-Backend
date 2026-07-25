package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RecentBookSearchRepositoryTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ZSetOperations<String, String> zSetOperations;
  @InjectMocks private RecentBookSearchRepository recentBookSearchRepository;

  @Test
  void saveUsesMemberKeyMaximumFiveAndThirtyDayTtl() {
    recentBookSearchRepository.save(7L, "혼모노");

    verify(stringRedisTemplate)
        .execute(
            org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
            eq(List.of("recent-searches:member:7")),
            anyString(),
            eq("혼모노"),
            eq("5"),
            eq("2592000"));
  }

  @Test
  void findAllByMemberIdReturnsMaximumFiveSearchesInDescendingScoreOrder() {
    Set<ZSetOperations.TypedTuple<String>> tuples =
        new LinkedHashSet<>(
            List.of(
                recentSearchTuple("혼모노", 1_721_000_000_000D),
                recentSearchTuple("미움받을 용기", 1_720_000_000_000D)));
    given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
    given(zSetOperations.reverseRangeWithScores("recent-searches:member:7", 0, 4))
        .willReturn(tuples);

    List<RecentBookSearchRepository.RecentSearch> result =
        recentBookSearchRepository.findAllByMemberId(7L);

    assertThat(result)
        .extracting(
            RecentBookSearchRepository.RecentSearch::keyword,
            RecentBookSearchRepository.RecentSearch::searchedAtEpochMillis)
        .containsExactly(tuple("혼모노", 1_721_000_000_000L), tuple("미움받을 용기", 1_720_000_000_000L));
  }

  @Test
  void findAllByMemberIdReturnsEmptyListWhenKeyDoesNotExist() {
    given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
    given(zSetOperations.reverseRangeWithScores("recent-searches:member:7", 0, 4)).willReturn(null);

    assertThat(recentBookSearchRepository.findAllByMemberId(7L)).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private ZSetOperations.TypedTuple<String> recentSearchTuple(String keyword, double score) {
    ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
    given(tuple.getValue()).willReturn(keyword);
    given(tuple.getScore()).willReturn(score);
    return tuple;
  }
}
