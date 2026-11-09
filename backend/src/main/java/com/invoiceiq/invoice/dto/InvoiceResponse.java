package com.invoiceiq.invoice.dto;

import com.invoiceiq.invoice.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        InvoiceStatus status,
        String originalFileName,
        String contentType,
        Long fileSize,
        String fileHash,
        String fileUrl,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        String vendorName,
        String vendorGstin,
        String buyerGstin,
        BigDecimal subtotal,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        BigDecimal overallConfidence,
        String errorMessage,
        boolean hasValidationErrors,
        boolean hasValidationWarnings,
        Map<String, Object> metadata,
        List<LineItemResponse> lineItems,
        Instant createdAt,
        Instant updatedAt
) {}
