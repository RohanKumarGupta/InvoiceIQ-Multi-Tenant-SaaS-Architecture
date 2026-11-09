package com.invoiceiq.invoice.dto;

import java.time.Instant;
import java.util.UUID;

public record PresignedUrlResponse(
        UUID invoiceId,
        String url,
        int expiresInSeconds,
        Instant expiresAt
) {}
