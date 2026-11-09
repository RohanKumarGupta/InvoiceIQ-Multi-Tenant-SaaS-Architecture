package com.invoiceiq.validation.service;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceStatus;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.validation.model.InvoiceValidation;
import com.invoiceiq.validation.model.ValidationResult;
import com.invoiceiq.validation.model.ValidationStatus;
import com.invoiceiq.validation.repository.InvoiceValidationRepository;
import com.invoiceiq.validation.rule.ValidationRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationEngine {

    private final List<ValidationRule> validationRules;
    private final InvoiceValidationRepository validationRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public List<InvoiceValidation> evaluateAndPersist(Invoice invoice) {
        UUID invoiceId = invoice.getId();
        UUID tenantId = invoice.getTenantId();

        log.info("Running validation engine for invoice {} (tenant: {}, {} rules registered)",
                invoiceId, tenantId, validationRules.size());

        // 1. Purge stale validations
        validationRepository.deleteAllByTenantIdAndInvoiceId(tenantId, invoiceId);

        List<InvoiceValidation> entities = new ArrayList<>();
        boolean hasErrors = false;
        boolean hasWarnings = false;

        // 2. Execute all pluggable rules
        for (ValidationRule rule : validationRules) {
            try {
                ValidationResult result = rule.validate(invoice);

                if (result.status() == ValidationStatus.FAIL) {
                    hasErrors = true;
                } else if (result.status() == ValidationStatus.WARN) {
                    hasWarnings = true;
                }

                InvoiceValidation entity = InvoiceValidation.builder()
                        .invoiceId(invoiceId)
                        .ruleCode(result.ruleCode())
                        .ruleName(result.ruleName())
                        .status(result.status())
                        .field(result.field())
                        .message(result.message())
                        .details(result.details())
                        .build();
                entity.setTenantId(tenantId);
                entities.add(entity);

            } catch (Exception e) {
                log.error("Rule {} execution failed on invoice {}: {}", rule.getRuleCode(), invoiceId, e.getMessage(), e);
                hasWarnings = true;
                InvoiceValidation errorEntity = InvoiceValidation.builder()
                        .invoiceId(invoiceId)
                        .ruleCode(rule.getRuleCode())
                        .ruleName(rule.getRuleName())
                        .status(ValidationStatus.WARN)
                        .message("Validation rule crashed: " + e.getMessage())
                        .build();
                errorEntity.setTenantId(tenantId);
                entities.add(errorEntity);
            }
        }

        List<InvoiceValidation> saved = validationRepository.saveAll(entities);

        // 3. Update invoice flags and transition status
        invoice.setHasValidationErrors(hasErrors);
        invoice.setHasValidationWarnings(hasWarnings);

        // Transition from EXTRACTED to NEEDS_REVIEW
        if (invoice.getStatus() == InvoiceStatus.EXTRACTED || invoice.getStatus() == InvoiceStatus.PROCESSING) {
            invoice.setStatus(InvoiceStatus.NEEDS_REVIEW);
        }

        invoiceRepository.save(invoice);
        log.info("Completed validation engine for invoice {}: errors={}, warnings={}, status={}",
                invoiceId, hasErrors, hasWarnings, invoice.getStatus());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InvoiceValidation> getValidations(UUID tenantId, UUID invoiceId) {
        return validationRepository.findAllByTenantIdAndInvoiceIdOrderByCreatedAtAsc(tenantId, invoiceId);
    }
}
