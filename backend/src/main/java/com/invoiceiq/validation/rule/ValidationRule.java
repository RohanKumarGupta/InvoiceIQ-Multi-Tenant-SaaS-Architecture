package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.validation.model.ValidationResult;

public interface ValidationRule {

    String getRuleCode();

    String getRuleName();

    ValidationResult validate(Invoice invoice);
}
