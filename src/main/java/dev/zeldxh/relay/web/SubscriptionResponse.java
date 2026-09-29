package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Subscription;

import java.time.Instant;

public record SubscriptionResponse(Long id, Long topicId, Long endpointId, Instant createdAt) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getTopic().getId(),
                subscription.getEndpoint().getId(),
                subscription.getCreatedAt()
        );
    }
}
