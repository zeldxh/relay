package dev.zeldxh.relay.audit;

import dev.zeldxh.relay.domain.AuditLog;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(Organization organization, String action, String details) {
        auditLogRepository.save(new AuditLog(organization, action, details));
    }
}
