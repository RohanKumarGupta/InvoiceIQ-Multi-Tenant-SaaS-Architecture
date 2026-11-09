package com.invoiceiq.invoice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LineItemResponse(
        UUID id,
        Integer lineNumber,
        String description,
        String hsnSac,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        BigDecimal confidence
) {}
