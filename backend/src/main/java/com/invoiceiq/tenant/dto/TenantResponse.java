package com.invoiceiq.tenant.dto;

import com.invoiceiq.tenant.model.TenantPlan;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantPlan plan,
        Map<String, Object> settings,
        Instant createdAt
) {}
