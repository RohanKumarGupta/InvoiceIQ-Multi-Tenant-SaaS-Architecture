package com.invoiceiq.invoice.dto;

import com.invoiceiq.invoice.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
        UUID id,
        InvoiceStatus status,
        String originalFileName,
        String contentType,
        Long fileSize,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String vendorName,
        String vendorGstin,
        BigDecimal totalAmount,
        String currency,
        BigDecimal overallConfidence,
        boolean hasValidationErrors,
        boolean hasValidationWarnings,
        Instant createdAt
) {}
