package dev.zeldxh.relay.web;

import dev.zeldxh.relay.domain.AuditLog;

import java.time.Instant;

public record AuditLogResponse(Long id, String action, String details, Instant createdAt) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(auditLog.getId(), auditLog.getAction(), auditLog.getDetails(), auditLog.getCreatedAt());
    }
}
