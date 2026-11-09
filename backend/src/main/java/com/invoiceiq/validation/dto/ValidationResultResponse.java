package com.invoiceiq.validation.dto;

import com.invoiceiq.validation.model.ValidationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ValidationResultResponse(
        UUID id,
        UUID invoiceId,
        String ruleCode,
        String ruleName,
        ValidationStatus status,
        String field,
        String message,
        Map<String, Object> details,
        Instant createdAt
) {}
