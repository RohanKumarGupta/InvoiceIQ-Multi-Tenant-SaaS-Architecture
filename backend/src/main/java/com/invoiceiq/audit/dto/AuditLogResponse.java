package com.invoiceiq.audit.dto;

import com.invoiceiq.audit.model.AuditAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityType,
        UUID entityId,
        AuditAction action,
        UUID actorId,
        String actorEmail,
        String actorRole,
        Map<String, Object> changes,
        String reason,
        String ipAddress,
        Instant createdAt
) {}
