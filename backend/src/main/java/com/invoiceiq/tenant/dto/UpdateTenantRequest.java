package com.invoiceiq.tenant.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateTenantRequest(
        @Size(min = 2, max = 255) String name,
        Map<String, Object> settings
) {}
