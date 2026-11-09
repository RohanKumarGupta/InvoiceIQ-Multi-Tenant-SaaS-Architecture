package com.invoiceiq.invoice.model;

public enum InvoiceStatus {
    UPLOADED,
    PROCESSING,
    EXTRACTED,
    NEEDS_REVIEW,
    APPROVED,
    REJECTED,
    EXPORTED,
    FAILED
}
