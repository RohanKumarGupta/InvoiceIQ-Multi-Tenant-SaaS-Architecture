package com.invoiceiq.invoice.repository;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Invoice> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<Invoice> findAllByTenantIdAndStatus(UUID tenantId, InvoiceStatus status, Pageable pageable);

    boolean existsByTenantIdAndFileHash(UUID tenantId, String fileHash);

    boolean existsByTenantIdAndFileHashAndIdNot(UUID tenantId, String fileHash, UUID id);

    @Query("""
           SELECT i FROM Invoice i
           WHERE i.tenantId = :tenantId
             AND i.id != :id
             AND LOWER(i.invoiceNumber) = LOWER(:invoiceNumber)
             AND (i.vendorGstin = :vendorGstin OR LOWER(i.vendorName) = LOWER(:vendorName))
             AND i.totalAmount = :totalAmount
           """)
    Optional<Invoice> findPotentialDuplicate(
            @Param("tenantId") UUID tenantId,
            @Param("id") UUID id,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("vendorGstin") String vendorGstin,
            @Param("vendorName") String vendorName,
            @Param("totalAmount") java.math.BigDecimal totalAmount);

    @Query("""
           SELECT i FROM Invoice i
           WHERE i.tenantId = :tenantId
             AND i.id != :id
             AND (i.vendorGstin = :vendorGstin OR LOWER(i.vendorName) = LOWER(:vendorName))
             AND i.totalAmount IS NOT NULL
             AND i.status IN ('APPROVED', 'EXTRACTED', 'EXPORTED')
           """)
    java.util.List<Invoice> findHistoricalInvoicesForVendor(
            @Param("tenantId") UUID tenantId,
            @Param("id") UUID id,
            @Param("vendorGstin") String vendorGstin,
            @Param("vendorName") String vendorName);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.id = :id AND i.tenantId = :tenantId")
    Optional<Invoice> findByIdAndTenantIdWithLineItems(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
