package dev.zeldxh.relay.support;

import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import dev.zeldxh.relay.security.PasswordHasher;

public final class OrganizationFixtures {

    private OrganizationFixtures() {
    }

    public record Seeded(Organization organization, String rawApiKey) {
        public String authorizationHeader() {
            return "Bearer " + rawApiKey;
        }
    }

    public static Seeded seed(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher, String name) {
        String rawKey = apiKeyHasher.generateKey();
        Organization organization = organizationRepository.save(new Organization(name, apiKeyHasher.hash(rawKey)));
        return new Seeded(organization, rawKey);
    }

    public static Seeded seedWithCredentials(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher,
                                              PasswordHasher passwordHasher, String name, String email, String rawPassword) {
        String rawKey = apiKeyHasher.generateKey();
        Organization organization = new Organization(name, apiKeyHasher.hash(rawKey));
        organization.setDashboardCredentials(email, passwordHasher.hash(rawPassword));
        organization = organizationRepository.save(organization);
        return new Seeded(organization, rawKey);
    }
}
