package dev.zeldxh.relay.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Fixed-window rate limiter backed by Redis, so the limit is enforced consistently
 * across every instance of the app rather than per-process.
 */
@Component
public class RateLimiterService {

    // Atomically increments the per-endpoint counter for the current 1s window and, on the
    // first hit of a new window, sets it to expire — avoiding a race between INCR and EXPIRE.
    private static final String SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            else
                return 1
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    public boolean tryConsume(Long endpointId, int ratePerSecond) {
        return tryConsume("relay:ratelimit:endpoint:" + endpointId, ratePerSecond, Duration.ofSeconds(1));
    }

    /** General form: an arbitrary bucket key, a request limit, and how long that limit covers. */
    public boolean tryConsume(String bucketKey, int limit, Duration window) {
        Long allowed = redisTemplate.execute(
                script, List.of(bucketKey), String.valueOf(limit), String.valueOf(window.toMillis()));
        return allowed != null && allowed == 1L;
    }
}
