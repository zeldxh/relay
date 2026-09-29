package dev.zeldxh.relay.web;

import dev.zeldxh.relay.audit.AuditLogService;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.security.ApiKeyAuthFilter;
import dev.zeldxh.relay.signing.HmacSigner;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/endpoints")
public class EndpointController {

    private static final Logger log = LoggerFactory.getLogger(EndpointController.class);
    private static final String PING_PAYLOAD = "{\"type\":\"ping\"}";

    private final EndpointRepository endpointRepository;
    private final HmacSigner hmacSigner;
    private final AuditLogService auditLogService;
    private final RestClient restClient;

    public EndpointController(EndpointRepository endpointRepository, HmacSigner hmacSigner,
                               AuditLogService auditLogService, RestClient restClient) {
        this.endpointRepository = endpointRepository;
        this.hmacSigner = hmacSigner;
        this.auditLogService = auditLogService;
        this.restClient = restClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse create(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                    @Valid @RequestBody CreateEndpointRequest request) {
        Endpoint endpoint = new Endpoint(organization, request.name(), request.url(), hmacSigner.generateSecret());
        if (request.rateLimitPerSecond() != null) {
            endpoint.setRateLimitPerSecond(request.rateLimitPerSecond());
        }
        endpoint = endpointRepository.save(endpoint);
        auditLogService.record(organization, "endpoint.created", "name=" + request.name() + ", url=" + request.url());
        return EndpointResponse.from(endpoint);
    }

    @GetMapping
    public List<EndpointResponse> list(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization) {
        return endpointRepository.findByOrganization(organization).stream().map(EndpointResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EndpointResponse get(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @PathVariable Long id) {
        return endpointRepository.findByIdAndOrganization(id, organization)
                .map(EndpointResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));
    }

    @PostMapping("/{id}/verify")
    public EndpointResponse verify(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                    @PathVariable Long id) {
        Endpoint endpoint = endpointRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));

        String signature = hmacSigner.sign(PING_PAYLOAD, endpoint.getSecret());
        try {
            restClient.post()
                    .uri(endpoint.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSigner.SIGNATURE_HEADER, signature)
                    .body(PING_PAYLOAD)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Verification ping failed for endpoint {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "endpoint did not respond successfully to the verification ping");
        }

        endpoint.markVerified();
        endpoint = endpointRepository.save(endpoint);
        auditLogService.record(organization, "endpoint.verified", "endpointId=" + endpoint.getId());
        return EndpointResponse.from(endpoint);
    }
}
