package com.invoiceiq.invoice.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectInvoiceRequest(
        @NotBlank(message = "Rejection reason is required") String reason
) {}
