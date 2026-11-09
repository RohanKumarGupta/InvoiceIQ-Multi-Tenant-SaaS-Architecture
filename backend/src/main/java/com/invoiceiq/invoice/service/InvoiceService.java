package com.invoiceiq.invoice.service;

import com.invoiceiq.audit.model.AuditAction;
import com.invoiceiq.audit.service.AuditService;
import com.invoiceiq.common.exception.ApiException;
import com.invoiceiq.common.exception.ResourceNotFoundException;
import com.invoiceiq.common.ratelimit.RateLimitingService;
import com.invoiceiq.invoice.dto.*;
import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceLineItem;
import com.invoiceiq.invoice.model.InvoiceStatus;
import com.invoiceiq.invoice.repository.InvoiceLineItemRepository;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.pipeline.event.InvoiceUploadedEvent;
import com.invoiceiq.pipeline.producer.InvoicePipelineProducer;
import com.invoiceiq.security.UserPrincipal;
import com.invoiceiq.storage.config.MinioConfig;
import com.invoiceiq.storage.service.StorageService;
import com.invoiceiq.validation.service.ValidationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final StorageService storageService;
    private final FileValidationService fileValidationService;
    private final VirusScanService virusScanService;
    private final InvoicePipelineProducer pipelineProducer;
    private final MinioConfig minioConfig;
    private final RateLimitingService rateLimitingService;
    private final ValidationEngine validationEngine;
    private final AuditService auditService;

    @Transactional
    public InvoiceResponse uploadInvoice(UUID tenantId, UUID userId, MultipartFile file) {
        // 0. Rate limit enforcement per tenant
        rateLimitingService.checkUploadRateLimit(tenantId);

        // 1. Magic byte & size validation
        String validatedContentType = fileValidationService.validateAndDetectContentType(file);

        // 2. Compute SHA-256 hash & read file bytes
        byte[] fileBytes;
        String sha256Hex;
        try {
            fileBytes = file.getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            sha256Hex = HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to compute SHA-256 for file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading uploaded file bytes");
        }

        // 3. Virus scanning hook
        if (!virusScanService.isClean(fileBytes, file.getOriginalFilename())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File failed antivirus inspection");
        }

        UUID invoiceId = UUID.randomUUID();
        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        String objectKey = String.format("tenants/%s/invoices/%s/%s", tenantId, invoiceId, originalFileName);

        // 4. Upload to MinIO object storage
        try (InputStream is = file.getInputStream()) {
            storageService.upload(objectKey, is, file.getSize(), validatedContentType);
        } catch (Exception e) {
            log.error("Storage upload failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to storage");
        }

        // 5. Persist Invoice entity
        Invoice invoice = Invoice.builder()
                .status(InvoiceStatus.UPLOADED)
                .fileKey(objectKey)
                .originalFileName(originalFileName)
                .contentType(validatedContentType)
                .fileSize(file.getSize())
                .fileHash(sha256Hex)
                .createdBy(userId)
                .build();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Persisted invoice {} with status UPLOADED for tenant {}", saved.getId(), tenantId);

        // 6. Publish async event to RabbitMQ
        InvoiceUploadedEvent event = new InvoiceUploadedEvent(
                saved.getId(),
                tenantId,
                objectKey,
                originalFileName,
                validatedContentType,
                sha256Hex,
                userId,
                Instant.now()
        );
        pipelineProducer.publishInvoiceUploaded(event);

        // 7. Generate view URL
        String presignedUrl = storageService.generatePresignedUrl(objectKey, minioConfig.getPresignedExpirySeconds());
        return mapToResponse(saved, presignedUrl);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID tenantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        String presignedUrl = storageService.generatePresignedUrl(invoice.getFileKey(), minioConfig.getPresignedExpirySeconds());
        return mapToResponse(invoice, presignedUrl);
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse getPresignedDownloadUrl(UUID tenantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        int expiry = minioConfig.getPresignedExpirySeconds();
        String url = storageService.generatePresignedUrl(invoice.getFileKey(), expiry);
        return new PresignedUrlResponse(invoiceId, url, expiry, Instant.now().plusSeconds(expiry));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> listInvoices(UUID tenantId, InvoiceStatus status, Pageable pageable) {
        Page<Invoice> page = (status == null)
                ? invoiceRepository.findAllByTenantId(tenantId, pageable)
                : invoiceRepository.findAllByTenantIdAndStatus(tenantId, status, pageable);

        return page.map(this::mapToSummaryResponse);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID tenantId, UUID invoiceId, UpdateInvoiceRequest request, UserPrincipal actor, String ipAddress) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        Map<String, Object> changes = new HashMap<>();

        if (request.invoiceNumber() != null && !request.invoiceNumber().equals(invoice.getInvoiceNumber())) {
            recordDiff("invoiceNumber", invoice.getInvoiceNumber(), request.invoiceNumber(), changes);
            invoice.setInvoiceNumber(request.invoiceNumber());
        }
        if (request.invoiceDate() != null && !request.invoiceDate().equals(invoice.getInvoiceDate())) {
            recordDiff("invoiceDate", invoice.getInvoiceDate(), request.invoiceDate(), changes);
            invoice.setInvoiceDate(request.invoiceDate());
        }
        if (request.dueDate() != null && !request.dueDate().equals(invoice.getDueDate())) {
            recordDiff("dueDate", invoice.getDueDate(), request.dueDate(), changes);
            invoice.setDueDate(request.dueDate());
        }
        if (request.currency() != null && !request.currency().equalsIgnoreCase(invoice.getCurrency())) {
            recordDiff("currency", invoice.getCurrency(), request.currency().toUpperCase(), changes);
            invoice.setCurrency(request.currency().toUpperCase());
        }
        if (request.vendorName() != null && !request.vendorName().equals(invoice.getVendorName())) {
            recordDiff("vendorName", invoice.getVendorName(), request.vendorName(), changes);
            invoice.setVendorName(request.vendorName());
        }
        if (request.vendorGstin() != null && !request.vendorGstin().equalsIgnoreCase(invoice.getVendorGstin())) {
            recordDiff("vendorGstin", invoice.getVendorGstin(), request.vendorGstin().toUpperCase(), changes);
            invoice.setVendorGstin(request.vendorGstin().toUpperCase());
        }
        if (request.buyerGstin() != null && !request.buyerGstin().equalsIgnoreCase(invoice.getBuyerGstin())) {
            recordDiff("buyerGstin", invoice.getBuyerGstin(), request.buyerGstin().toUpperCase(), changes);
            invoice.setBuyerGstin(request.buyerGstin().toUpperCase());
        }
        if (request.subtotal() != null && !request.subtotal().equals(invoice.getSubtotal())) {
            recordDiff("subtotal", invoice.getSubtotal(), request.subtotal(), changes);
            invoice.setSubtotal(request.subtotal());
        }
        if (request.cgstAmount() != null && !request.cgstAmount().equals(invoice.getCgstAmount())) {
            recordDiff("cgstAmount", invoice.getCgstAmount(), request.cgstAmount(), changes);
            invoice.setCgstAmount(request.cgstAmount());
        }
        if (request.sgstAmount() != null && !request.sgstAmount().equals(invoice.getSgstAmount())) {
            recordDiff("sgstAmount", invoice.getSgstAmount(), request.sgstAmount(), changes);
            invoice.setSgstAmount(request.sgstAmount());
        }
        if (request.igstAmount() != null && !request.igstAmount().equals(invoice.getIgstAmount())) {
            recordDiff("igstAmount", invoice.getIgstAmount(), request.igstAmount(), changes);
            invoice.setIgstAmount(request.igstAmount());
        }
        if (request.totalAmount() != null && !request.totalAmount().equals(invoice.getTotalAmount())) {
            recordDiff("totalAmount", invoice.getTotalAmount(), request.totalAmount(), changes);
            invoice.setTotalAmount(request.totalAmount());
        }

        // Update line items if provided
        if (request.lineItems() != null) {
            recordDiff("lineItems", invoice.getLineItems().size() + " items", request.lineItems().size() + " items", changes);
            lineItemRepository.deleteAllByInvoiceIdAndTenantId(invoiceId, tenantId);
            invoice.getLineItems().clear();

            for (UpdateInvoiceRequest.UpdateLineItemRequest itemReq : request.lineItems()) {
                InvoiceLineItem item = InvoiceLineItem.builder()
                        .invoice(invoice)
                        .lineNumber(itemReq.lineNumber() != null ? itemReq.lineNumber() : 1)
                        .description(itemReq.description() != null ? itemReq.description() : "")
                        .hsnSac(itemReq.hsnSac())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .taxRate(itemReq.taxRate())
                        .cgstAmount(itemReq.cgstAmount())
                        .sgstAmount(itemReq.sgstAmount())
                        .igstAmount(itemReq.igstAmount())
                        .totalAmount(itemReq.totalAmount() != null ? itemReq.totalAmount() : java.math.BigDecimal.ZERO)
                        .confidence(java.math.BigDecimal.ONE)
                        .build();
                item.setTenantId(tenantId);
                invoice.addLineItem(item);
            }
        }

        // Persist update
        Invoice updated = invoiceRepository.save(invoice);

        // Record audit trail
        auditService.recordAction(
                tenantId,
                "INVOICE",
                invoiceId,
                AuditAction.UPDATED,
                actor,
                changes,
                "Invoice fields updated by reviewer",
                ipAddress
        );

        // Re-evaluate validation engine on edited invoice data
        validationEngine.evaluateAndPersist(updated);

        String presignedUrl = storageService.generatePresignedUrl(updated.getFileKey(), minioConfig.getPresignedExpirySeconds());
        return mapToResponse(updated, presignedUrl);
    }

    @Transactional
    public InvoiceResponse approveInvoice(UUID tenantId, UUID invoiceId, ApproveInvoiceRequest request, UserPrincipal actor, String ipAddress) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setErrorMessage(null);
        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> changes = new HashMap<>();
        recordDiff("status", oldStatus, InvoiceStatus.APPROVED, changes);
        if (request != null && request.notes() != null) {
            changes.put("notes", request.notes());
        }

        auditService.recordAction(
                tenantId,
                "INVOICE",
                invoiceId,
                AuditAction.APPROVED,
                actor,
                changes,
                request != null ? request.notes() : "Invoice approved",
                ipAddress
        );

        String presignedUrl = storageService.generatePresignedUrl(saved.getFileKey(), minioConfig.getPresignedExpirySeconds());
        return mapToResponse(saved, presignedUrl);
    }

    @Transactional
    public InvoiceResponse rejectInvoice(UUID tenantId, UUID invoiceId, RejectInvoiceRequest request, UserPrincipal actor, String ipAddress) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.REJECTED);
        invoice.setErrorMessage("Rejected: " + request.reason());
        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> changes = new HashMap<>();
        recordDiff("status", oldStatus, InvoiceStatus.REJECTED, changes);
        changes.put("reason", request.reason());

        auditService.recordAction(
                tenantId,
                "INVOICE",
                invoiceId,
                AuditAction.REJECTED,
                actor,
                changes,
                request.reason(),
                ipAddress
        );

        String presignedUrl = storageService.generatePresignedUrl(saved.getFileKey(), minioConfig.getPresignedExpirySeconds());
        return mapToResponse(saved, presignedUrl);
    }

    @Transactional
    public InvoiceResponse revalidateInvoice(UUID tenantId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        validationEngine.evaluateAndPersist(invoice);
        Invoice refreshed = invoiceRepository.findByIdAndTenantIdWithLineItems(invoiceId, tenantId).orElse(invoice);

        String presignedUrl = storageService.generatePresignedUrl(refreshed.getFileKey(), minioConfig.getPresignedExpirySeconds());
        return mapToResponse(refreshed, presignedUrl);
    }

    private void recordDiff(String field, Object oldVal, Object newVal, Map<String, Object> changes) {
        Map<String, Object> diff = new HashMap<>();
        diff.put("old", oldVal != null ? oldVal.toString() : null);
        diff.put("new", newVal != null ? newVal.toString() : null);
        changes.put(field, diff);
    }

    private InvoiceResponse mapToResponse(Invoice invoice, String fileUrl) {
        List<LineItemResponse> lineItems = (invoice.getLineItems() == null) ? List.of() :
                invoice.getLineItems().stream()
                        .map(this::mapToLineItemResponse)
                        .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getTenantId(),
                invoice.getStatus(),
                invoice.getOriginalFileName(),
                invoice.getContentType(),
                invoice.getFileSize(),
                invoice.getFileHash(),
                fileUrl,
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getCurrency(),
                invoice.getVendorName(),
                invoice.getVendorGstin(),
                invoice.getBuyerGstin(),
                invoice.getSubtotal(),
                invoice.getCgstAmount(),
                invoice.getSgstAmount(),
                invoice.getIgstAmount(),
                invoice.getTotalAmount(),
                invoice.getOverallConfidence(),
                invoice.getErrorMessage(),
                invoice.isHasValidationErrors(),
                invoice.isHasValidationWarnings(),
                invoice.getMetadata(),
                lineItems,
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    private InvoiceSummaryResponse mapToSummaryResponse(Invoice invoice) {
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getStatus(),
                invoice.getOriginalFileName(),
                invoice.getContentType(),
                invoice.getFileSize(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getVendorName(),
                invoice.getVendorGstin(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getOverallConfidence(),
                invoice.isHasValidationErrors(),
                invoice.isHasValidationWarnings(),
                invoice.getCreatedAt()
        );
    }

    private LineItemResponse mapToLineItemResponse(InvoiceLineItem item) {
        return new LineItemResponse(
                item.getId(),
                item.getLineNumber(),
                item.getDescription(),
                item.getHsnSac(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTaxRate(),
                item.getCgstAmount(),
                item.getSgstAmount(),
                item.getIgstAmount(),
                item.getTotalAmount(),
                item.getConfidence()
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
