package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Organization;

public record AuthResponse(Long organizationId, String organizationName, String email, String apiKey) {

    public static AuthResponse from(Organization organization, String apiKey) {
        return new AuthResponse(organization.getId(), organization.getName(), organization.getEmail(), apiKey);
    }
}
