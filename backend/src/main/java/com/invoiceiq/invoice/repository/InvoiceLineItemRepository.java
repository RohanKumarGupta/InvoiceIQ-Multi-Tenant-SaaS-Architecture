package com.invoiceiq.invoice.repository;

import com.invoiceiq.invoice.model.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    List<InvoiceLineItem> findAllByInvoiceIdAndTenantIdOrderByLineNumberAsc(UUID invoiceId, UUID tenantId);

    void deleteAllByInvoiceIdAndTenantId(UUID invoiceId, UUID tenantId);
}
