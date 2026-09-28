package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Organization;

public record SessionResponse(Long organizationId, String organizationName, String email) {

    public static SessionResponse from(Organization organization) {
        return new SessionResponse(organization.getId(), organization.getName(), organization.getEmail());
    }
}
