package com.invoiceiq.invoice.controller;

import com.invoiceiq.audit.dto.AuditLogResponse;
import com.invoiceiq.audit.service.AuditService;
import com.invoiceiq.extraction.dto.AiUsageDto;
import com.invoiceiq.extraction.service.AiExtractionService;
import com.invoiceiq.invoice.dto.*;
import com.invoiceiq.invoice.model.InvoiceStatus;
import com.invoiceiq.invoice.service.InvoiceService;
import com.invoiceiq.security.UserPrincipal;
import com.invoiceiq.validation.dto.ValidationResultResponse;
import com.invoiceiq.validation.model.InvoiceValidation;
import com.invoiceiq.validation.service.ValidationEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Management", description = "Endpoints for uploading, inspecting, and managing tenant invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final AiExtractionService aiExtractionService;
    private final ValidationEngine validationEngine;
    private final AuditService auditService;

    @Operation(summary = "Upload an invoice document (PDF, PNG, JPEG)",
            description = "Validates magic bytes, uploads to secure MinIO storage, creates invoice record, and queues async extraction.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invoice uploaded successfully and queued for extraction"),
            @ApiResponse(responseCode = "400", description = "Invalid file type, corrupt headers, or virus scan failure"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks upload permissions (requires OWNER, ACCOUNTANT, or REVIEWER)")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> uploadInvoice(
            @Parameter(description = "Invoice file to upload (PDF, PNG, JPG up to 10MB)", required = true)
            @RequestParam("file") MultipartFile file) {

        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.uploadInvoice(principal.tenantId(), principal.userId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get detailed invoice by ID", description = "Returns full invoice details, line items, and a temporary pre-signed view URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice found and returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Invoice not found in current tenant")
    })
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.getInvoice(principal.tenantId(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get temporary pre-signed download URL for invoice file")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pre-signed URL generated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @GetMapping("/{id}/download-url")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        PresignedUrlResponse response = invoiceService.getPresignedDownloadUrl(principal.tenantId(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all invoices for tenant", description = "Returns paginated invoice summaries, optionally filtered by status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of invoices"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ResponseEntity<Page<InvoiceSummaryResponse>> listInvoices(
            @Parameter(description = "Filter by status (optional)")
            @RequestParam(required = false) InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserPrincipal principal = getCurrentUserPrincipal();
        Page<InvoiceSummaryResponse> invoices = invoiceService.listInvoices(principal.tenantId(), status, pageable);
        return ResponseEntity.ok(invoices);
    }

    @Operation(summary = "Trigger on-demand AI extraction for an invoice",
            description = "Triggers synchronous Claude vision extraction and refreshes line items and confidences.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI extraction executed successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PostMapping("/{id}/extract")
    public ResponseEntity<InvoiceResponse> extractInvoice(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        aiExtractionService.processInvoiceExtraction(id, principal.tenantId());
        InvoiceResponse response = invoiceService.getInvoice(principal.tenantId(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get AI token usage logs and cost breakdown for an invoice")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI usage logs retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/{id}/ai-usage")
    public ResponseEntity<List<AiUsageDto>> getAiUsageLogs(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        List<AiUsageDto> logs = aiExtractionService.getAiUsageLogs(principal.tenantId(), id);
        return ResponseEntity.ok(logs);
    }

    @Operation(summary = "Get all validation engine results for an invoice",
            description = "Returns rule-by-rule evaluation status (PASS/WARN/FAIL) and detailed messages.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validations retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/{id}/validations")
    public ResponseEntity<List<ValidationResultResponse>> getValidations(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        List<InvoiceValidation> validations = validationEngine.getValidations(principal.tenantId(), id);
        List<ValidationResultResponse> responses = validations.stream()
                .map(v -> new ValidationResultResponse(
                        v.getId(),
                        v.getInvoiceId(),
                        v.getRuleCode(),
                        v.getRuleName(),
                        v.getStatus(),
                        v.getField(),
                        v.getMessage(),
                        v.getDetails(),
                        v.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Manually re-trigger validation engine",
            description = "Re-evaluates all validation rules against current invoice values.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation rules re-executed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PostMapping("/{id}/validate")
    public ResponseEntity<InvoiceResponse> revalidate(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.revalidateInvoice(principal.tenantId(), id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update invoice fields and line items (manual review/correction)",
            description = "Saves edited fields, generates change diffs into the audit trail, and re-runs validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice updated and re-validated"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request,
            HttpServletRequest httpRequest) {

        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.updateInvoice(
                principal.tenantId(), id, request, principal, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve invoice", description = "Transitions invoice status to APPROVED and logs reviewer audit action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice approved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<InvoiceResponse> approveInvoice(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveInvoiceRequest request,
            HttpServletRequest httpRequest) {

        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.approveInvoice(
                principal.tenantId(), id, request, principal, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject invoice", description = "Transitions invoice status to REJECTED with reason and logs reviewer audit action.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invoice rejected"),
            @ApiResponse(responseCode = "400", description = "Rejection reason missing"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('OWNER', 'ACCOUNTANT', 'REVIEWER')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<InvoiceResponse> rejectInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody RejectInvoiceRequest request,
            HttpServletRequest httpRequest) {

        UserPrincipal principal = getCurrentUserPrincipal();
        InvoiceResponse response = invoiceService.rejectInvoice(
                principal.tenantId(), id, request, principal, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get audit trail history for an invoice",
            description = "Chronological list of all modifications, status changes, and reviewer actions with field-level diffs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit trail logs retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@PathVariable UUID id) {
        UserPrincipal principal = getCurrentUserPrincipal();
        List<AuditLogResponse> logs = auditService.getEntityAuditLogs(principal.tenantId(), "INVOICE", id);
        return ResponseEntity.ok(logs);
    }

    private UserPrincipal getCurrentUserPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
