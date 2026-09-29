package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.AuditLogRepository;
import dev.zeldxh.relay.security.ApiKeyAuthFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                        Pageable pageable) {
        return auditLogRepository.findByOrganization(organization, pageable).map(AuditLogResponse::from);
    }
}
