package com.invoiceiq.pipeline.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record InvoiceUploadedEvent(
        UUID invoiceId,
        UUID tenantId,
        String fileKey,
        String originalFileName,
        String contentType,
        String fileHash,
        UUID uploadedBy,
        Instant timestamp
) implements Serializable {}
