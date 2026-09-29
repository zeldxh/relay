package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;

import java.time.Instant;

public record DeliveryResponse(
        Long id,
        Long eventId,
        Long endpointId,
        String topic,
        String payload,
        DeliveryStatus status,
        int attemptCount,
        int maxAttempts,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Integer lastResponseStatus,
        Instant createdAt
) {
    public static DeliveryResponse from(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getEvent().getId(),
                delivery.getEndpoint().getId(),
                delivery.getEvent().getTopic().getName(),
                delivery.getEvent().getPayload(),
                delivery.getStatus(),
                delivery.getAttemptCount(),
                delivery.getMaxAttempts(),
                delivery.getNextAttemptAt(),
                delivery.getLastAttemptAt(),
                delivery.getLastResponseStatus(),
                delivery.getCreatedAt()
        );
    }
}
