package com.invoiceiq.audit.service;

import com.invoiceiq.audit.dto.AuditLogResponse;
import com.invoiceiq.audit.model.AuditAction;
import com.invoiceiq.audit.model.AuditLog;
import com.invoiceiq.audit.repository.AuditLogRepository;
import com.invoiceiq.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordAction(
            UUID tenantId,
            String entityType,
            UUID entityId,
            AuditAction action,
            UserPrincipal actor,
            Map<String, Object> changes,
            String reason,
            String ipAddress) {

        AuditLog logEntry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actor != null ? actor.userId() : null)
                .actorEmail(actor != null ? actor.email() : "SYSTEM")
                .actorRole(actor != null && actor.role() != null ? actor.role().name() : "SYSTEM")
                .changes(changes != null ? changes : Collections.emptyMap())
                .reason(reason)
                .ipAddress(ipAddress)
                .build();
        logEntry.setTenantId(tenantId);

        auditLogRepository.save(logEntry);
        log.info("Recorded audit log: action={}, entityType={}, entityId={}, actor={}",
                action, entityType, entityId, logEntry.getActorEmail());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getEntityAuditLogs(UUID tenantId, String entityType, UUID entityId) {
        return auditLogRepository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(tenantId, entityType, entityId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getAction(),
                entry.getActorId(),
                entry.getActorEmail(),
                entry.getActorRole(),
                entry.getChanges(),
                entry.getReason(),
                entry.getIpAddress(),
                entry.getCreatedAt()
        );
    }
}
