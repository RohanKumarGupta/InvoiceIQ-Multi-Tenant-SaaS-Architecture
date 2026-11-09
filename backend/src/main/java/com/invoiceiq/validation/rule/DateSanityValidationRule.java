package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.validation.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class DateSanityValidationRule implements ValidationRule {

    @Override
    public String getRuleCode() {
        return "DATE_SANITY";
    }

    @Override
    public String getRuleName() {
        return "Invoice Date & Due Date Sanity";
    }

    @Override
    public ValidationResult validate(Invoice invoice) {
        LocalDate invoiceDate = invoice.getInvoiceDate();
        LocalDate today = LocalDate.now();

        if (invoiceDate == null) {
            return ValidationResult.warn(
                    getRuleCode(), getRuleName(), "invoiceDate",
                    "Invoice date could not be extracted.",
                    Map.of("invoiceDate", "MISSING")
            );
        }

        // 1. Future Date Check
        if (invoiceDate.isAfter(today)) {
            return ValidationResult.fail(
                    getRuleCode(), getRuleName(), "invoiceDate",
                    String.format("Invoice date (%s) is in the future (today is %s).", invoiceDate, today),
                    Map.of("invoiceDate", invoiceDate, "today", today)
            );
        }

        // 2. Old Date Check (> 365 days)
        long daysOld = ChronoUnit.DAYS.between(invoiceDate, today);
        if (daysOld > 365) {
            Map<String, Object> details = new HashMap<>();
            details.put("invoiceDate", invoiceDate);
            details.put("daysOld", daysOld);

            return ValidationResult.warn(
                    getRuleCode(), getRuleName(), "invoiceDate",
                    String.format("Invoice date (%s) is %d days old (> 1 year). Indian GST Section 16(4) time-limit for claiming ITC may have expired.",
                            invoiceDate, daysOld),
                    details
            );
        }

        // 3. Due Date Sanity
        LocalDate dueDate = invoice.getDueDate();
        if (dueDate != null && dueDate.isBefore(invoiceDate)) {
            return ValidationResult.warn(
                    getRuleCode(), getRuleName(), "dueDate",
                    String.format("Due date (%s) is earlier than the invoice issuance date (%s).", dueDate, invoiceDate),
                    Map.of("invoiceDate", invoiceDate, "dueDate", dueDate)
            );
        }

        return ValidationResult.pass(getRuleCode(), getRuleName(), "Invoice date is within valid historical limits.");
    }
}
