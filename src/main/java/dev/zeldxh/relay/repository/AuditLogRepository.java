package dev.zeldxh.relay.repository;

import dev.zeldxh.relay.domain.AuditLog;
import dev.zeldxh.relay.domain.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByOrganization(Organization organization, Pageable pageable);
}
