package com.invoiceiq.extraction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiUsageDto(
        UUID id,
        UUID invoiceId,
        String modelName,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        BigDecimal estimatedCostUsd,
        long executionTimeMs,
        boolean successful,
        String errorDetails,
        Instant createdAt
) {}
