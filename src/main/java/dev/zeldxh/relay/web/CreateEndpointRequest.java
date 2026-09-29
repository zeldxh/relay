package dev.zeldxh.relay.web;

import jakarta.validation.constraints.NotBlank;

public record CreateEndpointRequest(
        @NotBlank String name,
        @NotBlank String url,
        Integer rateLimitPerSecond
) {
}
