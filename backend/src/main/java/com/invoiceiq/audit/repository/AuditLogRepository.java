package com.invoiceiq.audit.repository;

import com.invoiceiq.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID tenantId, String entityType, UUID entityId);

    List<AuditLog> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
