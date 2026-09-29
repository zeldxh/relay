package dev.zeldxh.relay.web;

import dev.zeldxh.relay.audit.AuditLogService;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.ratelimit.PublicEndpointRateLimiter;
import dev.zeldxh.relay.repository.OrganizationRepository;
import dev.zeldxh.relay.security.ApiKeyHasher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final AuditLogService auditLogService;
    private final PublicEndpointRateLimiter publicEndpointRateLimiter;

    public OrganizationController(OrganizationRepository organizationRepository, ApiKeyHasher apiKeyHasher,
                                   AuditLogService auditLogService, PublicEndpointRateLimiter publicEndpointRateLimiter) {
        this.organizationRepository = organizationRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.auditLogService = auditLogService;
        this.publicEndpointRateLimiter = publicEndpointRateLimiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request, HttpServletRequest httpRequest) {
        if (!publicEndpointRateLimiter.trySignup(httpRequest.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many organizations created from this address, try again later");
        }

        String rawKey = apiKeyHasher.generateKey();
        Organization organization = organizationRepository.save(new Organization(request.name(), apiKeyHasher.hash(rawKey)));
        auditLogService.record(organization, "organization.created", "name=" + request.name());
        return OrganizationResponse.from(organization, rawKey);
    }
}
