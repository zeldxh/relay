package dev.zeldxh.relay.support;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Gives a MockMvc call its own simulated client IP, instead of the default "127.0.0.1" every
 * MockMvc request otherwise shares. Needed for anything hitting a per-IP rate-limited endpoint
 * (signup, login, organization creation) - without it, unrelated tests sharing the same Redis
 * instance would exhaust each other's quota.
 */
public final class UniqueRemoteAddr {

    private UniqueRemoteAddr() {
    }

    public static RequestPostProcessor unique() {
        String address = String.valueOf(System.nanoTime());
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
