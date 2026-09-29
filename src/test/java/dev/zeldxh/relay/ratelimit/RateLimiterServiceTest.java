package dev.zeldxh.relay.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requires a Redis instance reachable at localhost:6379 (e.g. via `docker compose up -d redis`,
 * or the redis service container GitHub Actions spins up for CI).
 */
class RateLimiterServiceTest {

    private static LettuceConnectionFactory connectionFactory;
    private static RateLimiterService rateLimiter;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        rateLimiter = new RateLimiterService(template);
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
    }

    @Test
    void allowsUpToConfiguredCapacityThenBlocks() {
        long endpointId = System.nanoTime();
        int ratePerSecond = 2;

        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(endpointId, ratePerSecond)).isFalse();
    }

    @Test
    void tracksEachEndpointIndependently() {
        long endpointA = System.nanoTime();
        long endpointB = endpointA + 1;
        int ratePerSecond = 1;

        assertThat(rateLimiter.tryConsume(endpointA, ratePerSecond)).isTrue();
        assertThat(rateLimiter.tryConsume(endpointA, ratePerSecond)).isFalse();

        assertThat(rateLimiter.tryConsume(endpointB, ratePerSecond)).isTrue();
    }
}
