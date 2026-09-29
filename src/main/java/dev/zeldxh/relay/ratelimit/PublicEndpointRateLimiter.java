package dev.zeldxh.relay.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Per-IP rate limiting for the handful of endpoints anyone can call without an API key or
 * session - account creation and login. Without this, they're an open invitation to spam
 * signups or brute-force passwords.
 *
 * Keyed on the request's remote address, which is naive behind a reverse proxy (everything
 * would share the proxy's IP unless it forwards the real one via a trusted header) - fine for
 * a direct deployment, but worth revisiting alongside whatever's in front of this in
 * production.
 */
@Component
public class PublicEndpointRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int SIGNUP_LIMIT_PER_IP = 5;
    private static final int LOGIN_LIMIT_PER_IP = 10;

    private final RateLimiterService rateLimiterService;

    public PublicEndpointRateLimiter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /** Covers both POST /organizations and POST /auth/signup - both create an organization. */
    public boolean trySignup(String clientIp) {
        return rateLimiterService.tryConsume("relay:ratelimit:signup:" + clientIp, SIGNUP_LIMIT_PER_IP, WINDOW);
    }

    public boolean tryLogin(String clientIp) {
        return rateLimiterService.tryConsume("relay:ratelimit:login:" + clientIp, LOGIN_LIMIT_PER_IP, WINDOW);
    }
}
