package dev.zeldxh.relay.web;

import jakarta.validation.constraints.NotBlank;

public record CreateTopicRequest(@NotBlank String name) {
}
