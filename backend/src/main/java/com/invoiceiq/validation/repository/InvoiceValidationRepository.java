package com.invoiceiq.validation.repository;

import com.invoiceiq.validation.model.InvoiceValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceValidationRepository extends JpaRepository<InvoiceValidation, UUID> {

    List<InvoiceValidation> findAllByTenantIdAndInvoiceIdOrderByCreatedAtAsc(UUID tenantId, UUID invoiceId);

    void deleteAllByTenantIdAndInvoiceId(UUID tenantId, UUID invoiceId);
}
