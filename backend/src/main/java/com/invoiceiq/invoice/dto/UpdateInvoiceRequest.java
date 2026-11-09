package com.invoiceiq.invoice.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateInvoiceRequest(
        @Size(max = 100) String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        @Size(max = 10) String currency,
        @Size(max = 255) String vendorName,
        @Size(max = 15) String vendorGstin,
        @Size(max = 15) String buyerGstin,
        BigDecimal subtotal,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        List<UpdateLineItemRequest> lineItems
) {
    public record UpdateLineItemRequest(
            Integer lineNumber,
            String description,
            String hsnSac,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal taxRate,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount,
            BigDecimal igstAmount,
            BigDecimal totalAmount
    ) {}
}
