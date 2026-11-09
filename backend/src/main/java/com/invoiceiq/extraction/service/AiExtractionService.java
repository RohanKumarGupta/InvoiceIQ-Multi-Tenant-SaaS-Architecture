package com.invoiceiq.extraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceiq.common.exception.ResourceNotFoundException;
import com.invoiceiq.extraction.config.AiExtractionConfig;
import com.invoiceiq.extraction.dto.AiUsageDto;
import com.invoiceiq.extraction.dto.ExtractedInvoiceDto;
import com.invoiceiq.extraction.dto.ExtractedLineItemDto;
import com.invoiceiq.extraction.model.AiUsageLog;
import com.invoiceiq.extraction.repository.AiUsageLogRepository;
import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceLineItem;
import com.invoiceiq.invoice.model.InvoiceStatus;
import com.invoiceiq.invoice.repository.InvoiceLineItemRepository;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.storage.service.StorageService;
import com.invoiceiq.validation.service.ValidationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiExtractionService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final StorageService storageService;
    private final AnthropicVisionClient visionClient;
    private final AiExtractionConfig config;
    private final ObjectMapper objectMapper;
    private final ValidationEngine validationEngine;

    @Transactional
    public void processInvoiceExtraction(UUID invoiceId, UUID tenantId) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + invoiceId));

        log.info("Starting AI extraction for invoice: {} (tenant: {}, fileKey: {})", invoiceId, tenantId, invoice.getFileKey());

        byte[] fileBytes;
        try (InputStream is = storageService.download(invoice.getFileKey())) {
            fileBytes = is.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file from storage for invoice {}: {}", invoiceId, e.getMessage());
            handleExtractionFailure(invoice, "Storage download failed: " + e.getMessage(), 0, 0, 0);
            return;
        }

        AnthropicVisionClient.VisionResponse visionResponse;
        try {
            visionResponse = visionClient.callClaudeVision(fileBytes, invoice.getContentType());
        } catch (Exception e) {
            log.error("Vision API invocation failed for invoice {}: {}", invoiceId, e.getMessage());
            handleExtractionFailure(invoice, "Vision API error: " + e.getMessage(), 0, 0, 0);
            return;
        }

        // Calculate token costs
        BigDecimal cost = calculateCost(visionResponse.promptTokens(), visionResponse.completionTokens());

        // Log AI usage
        AiUsageLog usageLog = AiUsageLog.builder()
                .invoiceId(invoiceId)
                .modelName(config.getModel() + (visionResponse.isSynthetic() ? " (synthetic)" : ""))
                .promptTokens(visionResponse.promptTokens())
                .completionTokens(visionResponse.completionTokens())
                .totalTokens(visionResponse.promptTokens() + visionResponse.completionTokens())
                .estimatedCostUsd(cost)
                .executionTimeMs(visionResponse.executionTimeMs())
                .successful(true)
                .build();
        usageLog.setTenantId(tenantId);
        aiUsageLogRepository.save(usageLog);

        // Parse structured output
        ExtractedInvoiceDto extractedDto;
        try {
            String sanitizedJson = cleanJsonOutput(visionResponse.content());
            extractedDto = objectMapper.readValue(sanitizedJson, ExtractedInvoiceDto.class);
        } catch (Exception e) {
            log.error("Failed to parse extracted JSON for invoice {}: {}", invoiceId, e.getMessage());
            handleExtractionFailure(invoice, "JSON schema deserialization failure: " + e.getMessage(),
                    visionResponse.promptTokens(), visionResponse.completionTokens(), visionResponse.executionTimeMs());
            return;
        }

        // Apply extracted data to invoice
        applyExtractedData(invoice, extractedDto);
        invoice.setStatus(InvoiceStatus.EXTRACTED);
        invoice.setErrorMessage(null);

        // Replace line items
        lineItemRepository.deleteAllByInvoiceIdAndTenantId(invoiceId, tenantId);
        if (extractedDto.getLineItems() != null && !extractedDto.getLineItems().isEmpty()) {
            for (ExtractedLineItemDto itemDto : extractedDto.getLineItems()) {
                InvoiceLineItem lineItem = InvoiceLineItem.builder()
                        .invoice(invoice)
                        .lineNumber(itemDto.getLineNumber() != null ? itemDto.getLineNumber() : 1)
                        .description(itemDto.getDescription() != null ? itemDto.getDescription() : "Item")
                        .hsnSac(itemDto.getHsnSac())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(itemDto.getUnitPrice())
                        .taxRate(itemDto.getTaxRate())
                        .cgstAmount(itemDto.getCgstAmount())
                        .sgstAmount(itemDto.getSgstAmount())
                        .igstAmount(itemDto.getIgstAmount())
                        .totalAmount(itemDto.getTotalAmount() != null ? itemDto.getTotalAmount() : BigDecimal.ZERO)
                        .confidence(clampConfidence(itemDto.getConfidence()))
                        .build();
                lineItem.setTenantId(tenantId);
                invoice.addLineItem(lineItem);
            }
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Successfully extracted and saved invoice {} with status EXTRACTED (overall confidence: {})",
                savedInvoice.getId(), savedInvoice.getOverallConfidence());

        // Automatically trigger pluggable validation engine (Phase 4)
        validationEngine.evaluateAndPersist(savedInvoice);
    }

    @Transactional(readOnly = true)
    public List<AiUsageDto> getAiUsageLogs(UUID tenantId, UUID invoiceId) {
        return aiUsageLogRepository.findAllByTenantIdAndInvoiceIdOrderByCreatedAtDesc(tenantId, invoiceId)
                .stream()
                .map(log -> new AiUsageDto(
                        log.getId(),
                        log.getInvoiceId(),
                        log.getModelName(),
                        log.getPromptTokens(),
                        log.getCompletionTokens(),
                        log.getTotalTokens(),
                        log.getEstimatedCostUsd(),
                        log.getExecutionTimeMs(),
                        log.getSuccessful(),
                        log.getErrorDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }

    private void applyExtractedData(Invoice invoice, ExtractedInvoiceDto dto) {
        invoice.setInvoiceNumber(dto.getInvoiceNumber());
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate());
        if (dto.getCurrency() != null && !dto.getCurrency().isBlank()) {
            invoice.setCurrency(dto.getCurrency().trim().toUpperCase());
        }
        invoice.setVendorName(dto.getVendorName());
        invoice.setVendorGstin(cleanGstin(dto.getVendorGstin()));
        invoice.setBuyerGstin(cleanGstin(dto.getBuyerGstin()));
        invoice.setSubtotal(dto.getSubtotal());
        invoice.setCgstAmount(dto.getCgstAmount());
        invoice.setSgstAmount(dto.getSgstAmount());
        invoice.setIgstAmount(dto.getIgstAmount());
        invoice.setTotalAmount(dto.getTotalAmount());
        invoice.setOverallConfidence(clampConfidence(dto.getOverallConfidence()));

        // Store per-field confidences in metadata
        Map<String, Object> metadata = (invoice.getMetadata() != null) ? new HashMap<>(invoice.getMetadata()) : new HashMap<>();
        Map<String, Object> confidences = new HashMap<>();
        confidences.put("invoiceNumber", clampConfidence(dto.getInvoiceNumberConfidence()));
        confidences.put("invoiceDate", clampConfidence(dto.getInvoiceDateConfidence()));
        confidences.put("dueDate", clampConfidence(dto.getDueDateConfidence()));
        confidences.put("vendorName", clampConfidence(dto.getVendorNameConfidence()));
        confidences.put("vendorGstin", clampConfidence(dto.getVendorGstinConfidence()));
        confidences.put("buyerGstin", clampConfidence(dto.getBuyerGstinConfidence()));
        confidences.put("subtotal", clampConfidence(dto.getSubtotalConfidence()));
        confidences.put("totalAmount", clampConfidence(dto.getTotalAmountConfidence()));
        metadata.put("fieldConfidences", confidences);
        invoice.setMetadata(metadata);
    }

    private void handleExtractionFailure(Invoice invoice, String errorMessage, int promptTokens, int completionTokens, long latency) {
        log.warn("Handling extraction failure for invoice {}: {}", invoice.getId(), errorMessage);
        invoice.setStatus(InvoiceStatus.NEEDS_REVIEW); // Graceful fallback: allows manual entry
        invoice.setErrorMessage("AI Extraction Fallback: " + errorMessage);
        invoiceRepository.save(invoice);

        if (promptTokens > 0 || completionTokens > 0) {
            AiUsageLog failureLog = AiUsageLog.builder()
                    .invoiceId(invoice.getId())
                    .modelName(config.getModel())
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(promptTokens + completionTokens)
                    .estimatedCostUsd(calculateCost(promptTokens, completionTokens))
                    .executionTimeMs(latency)
                    .successful(false)
                    .errorDetails(errorMessage)
                    .build();
            failureLog.setTenantId(invoice.getTenantId());
            aiUsageLogRepository.save(failureLog);
        }
    }

    private String cleanJsonOutput(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private String cleanGstin(String gstin) {
        if (gstin == null) return null;
        String clean = gstin.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        return clean.isEmpty() ? null : clean;
    }

    private BigDecimal clampConfidence(BigDecimal conf) {
        if (conf == null) return BigDecimal.valueOf(0.50);
        if (conf.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (conf.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        return conf.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCost(int promptTokens, int completionTokens) {
        BigDecimal promptCost = config.getInputTokenCostPerMillion()
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);

        BigDecimal completionCost = config.getOutputTokenCostPerMillion()
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);

        return promptCost.add(completionCost);
    }
}
