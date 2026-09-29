package dev.zeldxh.relay.web;

import jakarta.validation.constraints.NotNull;

public record CreateSubscriptionRequest(@NotNull Long endpointId) {
}
