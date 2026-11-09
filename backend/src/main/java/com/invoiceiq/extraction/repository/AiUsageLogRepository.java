package com.invoiceiq.extraction.repository;

import com.invoiceiq.extraction.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    List<AiUsageLog> findAllByTenantIdAndInvoiceIdOrderByCreatedAtDesc(UUID tenantId, UUID invoiceId);

    List<AiUsageLog> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
