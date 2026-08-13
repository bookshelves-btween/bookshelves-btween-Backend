package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class ExternalBookRateLimitRepositoryIntegrationTest {

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static StringRedisTemplate redisTemplate;
  private static ExternalBookRateLimitRepository repository;

  @BeforeAll
  static void setUpRedis() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new StringRedisTemplate(connectionFactory);
    redisTemplate.afterPropertiesSet();
    repository = new ExternalBookRateLimitRepository(redisTemplate);
  }

  @AfterAll
  static void closeRedisConnection() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @Test
  void luaScriptSetsTtlIncrementsAndRestartsAfterExpiration() throws InterruptedException {
    Duration window = Duration.ofSeconds(1);
    String key = "book:rate-limit:v1:search:member:7";

    assertThat(repository.increment(7L, "search", window)).isEqualTo(1L);
    assertThat(redisTemplate.getExpire(key)).isPositive();
    assertThat(repository.increment(7L, "search", window)).isEqualTo(2L);

    Thread.sleep(1_200);

    assertThat(repository.increment(7L, "search", window)).isEqualTo(1L);
  }
}
