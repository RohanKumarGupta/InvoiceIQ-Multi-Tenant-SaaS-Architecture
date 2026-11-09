package com.invoiceiq.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedInvoiceDto {
    private String invoiceNumber;
    private BigDecimal invoiceNumberConfidence;

    private LocalDate invoiceDate;
    private BigDecimal invoiceDateConfidence;

    private LocalDate dueDate;
    private BigDecimal dueDateConfidence;

    private String currency;

    private String vendorName;
    private BigDecimal vendorNameConfidence;

    private String vendorGstin;
    private BigDecimal vendorGstinConfidence;

    private String buyerGstin;
    private BigDecimal buyerGstinConfidence;

    private BigDecimal subtotal;
    private BigDecimal subtotalConfidence;

    private BigDecimal cgstAmount;
    private BigDecimal cgstConfidence;

    private BigDecimal sgstAmount;
    private BigDecimal sgstConfidence;

    private BigDecimal igstAmount;
    private BigDecimal igstConfidence;

    private BigDecimal totalAmount;
    private BigDecimal totalAmountConfidence;

    private BigDecimal overallConfidence;

    @Builder.Default
    private List<ExtractedLineItemDto> lineItems = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> additionalAttributes = new HashMap<>();
}
