package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.validation.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DuplicateDetectionRule implements ValidationRule {

    private final InvoiceRepository invoiceRepository;

    @Override
    public String getRuleCode() {
        return "DUPLICATE_CHECK";
    }

    @Override
    public String getRuleName() {
        return "Duplicate Invoice Detection";
    }

    @Override
    public ValidationResult validate(Invoice invoice) {
        // 1. File Hash Check (exact bitwise duplicate)
        if (invoice.getFileHash() != null) {
            boolean hashExists = invoiceRepository.existsByTenantIdAndFileHashAndIdNot(
                    invoice.getTenantId(), invoice.getFileHash(), invoice.getId());

            if (hashExists) {
                Map<String, Object> details = new HashMap<>();
                details.put("duplicateType", "EXACT_FILE_HASH");
                details.put("fileHash", invoice.getFileHash());

                return ValidationResult.fail(
                        getRuleCode(), getRuleName(), "fileHash",
                        "Exact duplicate file detected. An identical invoice document has already been uploaded.",
                        details
                );
            }
        }

        // 2. Business Duplicate Check (Vendor + Invoice Number + Total Amount)
        if (invoice.getInvoiceNumber() != null && invoice.getTotalAmount() != null) {
            Optional<Invoice> duplicate = invoiceRepository.findPotentialDuplicate(
                    invoice.getTenantId(),
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    invoice.getVendorGstin() != null ? invoice.getVendorGstin() : "",
                    invoice.getVendorName() != null ? invoice.getVendorName() : "",
                    invoice.getTotalAmount()
            );

            if (duplicate.isPresent()) {
                Invoice match = duplicate.get();
                Map<String, Object> details = new HashMap<>();
                details.put("duplicateType", "BUSINESS_KEY_MATCH");
                details.put("matchingInvoiceId", match.getId());
                details.put("invoiceNumber", match.getInvoiceNumber());
                details.put("totalAmount", match.getTotalAmount());
                details.put("existingStatus", match.getStatus());

                return ValidationResult.fail(
                        getRuleCode(), getRuleName(), "invoiceNumber",
                        String.format("Duplicate invoice detected: Invoice #%s from vendor '%s' with amount %s already exists (ID: %s).",
                                invoice.getInvoiceNumber(),
                                invoice.getVendorName() != null ? invoice.getVendorName() : invoice.getVendorGstin(),
                                invoice.getTotalAmount(),
                                match.getId()),
                        details
                );
            }
        }

        return ValidationResult.pass(getRuleCode(), getRuleName(), "No duplicate invoice found by hash or business keys.");
    }
}
