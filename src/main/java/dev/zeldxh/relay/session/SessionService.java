package dev.zeldxh.relay.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Server-side dashboard login sessions, kept in Redis (not the {@code relay_} API keys,
 * which remain the bearer credential for the public delivery API).
 */
@Component
public class SessionService {

    public static final String COOKIE_NAME = "relay_session";
    private static final String KEY_PREFIX = "relay:session:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;

    public SessionService(StringRedisTemplate redisTemplate,
                           @Value("${relay.session.ttl-seconds:604800}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public String create(Long organizationId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String sessionId = HexFormat.of().formatHex(bytes);
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, String.valueOf(organizationId), ttl);
        return sessionId;
    }

    public Optional<Long> resolve(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        return Optional.ofNullable(value).map(Long::valueOf);
    }

    public void revoke(String sessionId) {
        if (sessionId != null) {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        }
    }

    public Duration getTtl() {
        return ttl;
    }
}
