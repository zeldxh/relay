package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Endpoint;

import java.time.Instant;

public record EndpointResponse(
        Long id,
        String name,
        String url,
        String secret,
        int rateLimitPerSecond,
        boolean verified,
        Instant verifiedAt,
        Instant createdAt
) {
    public static EndpointResponse from(Endpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.getSecret(),
                endpoint.getRateLimitPerSecond(),
                endpoint.isVerified(),
                endpoint.getVerifiedAt(),
                endpoint.getCreatedAt()
        );
    }
}
