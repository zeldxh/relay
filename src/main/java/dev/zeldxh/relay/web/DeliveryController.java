package dev.zeldxh.relay.web;

import dev.zeldxh.relay.audit.AuditLogService;
import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.DeliveryStatus;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.security.ApiKeyAuthFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final AuditLogService auditLogService;

    public DeliveryController(DeliveryRepository deliveryRepository, AuditLogService auditLogService) {
        this.deliveryRepository = deliveryRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> list(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                        @RequestParam(required = false) DeliveryStatus status, Pageable pageable) {
        Page<Delivery> page = status != null
                ? deliveryRepository.findByEndpoint_OrganizationAndStatus(organization, status, pageable)
                : deliveryRepository.findByEndpoint_Organization(organization, pageable);
        return page.map(DeliveryResponse::from);
    }

    @PostMapping("/{id}/replay")
    @Transactional
    public DeliveryResponse replay(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                    @PathVariable Long id) {
        Delivery delivery = deliveryRepository.findByIdAndEndpoint_Organization(id, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "delivery not found"));

        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "only failed deliveries can be replayed");
        }

        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextAttemptAt(Instant.now());
        delivery = deliveryRepository.save(delivery);

        auditLogService.record(organization, "delivery.replayed", "deliveryId=" + delivery.getId());
        return DeliveryResponse.from(delivery);
    }
}
