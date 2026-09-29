package dev.zeldxh.relay.web;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(@NotBlank String name) {
}
