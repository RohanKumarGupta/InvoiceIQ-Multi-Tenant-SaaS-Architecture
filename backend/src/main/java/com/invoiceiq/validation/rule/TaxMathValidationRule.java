package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.model.InvoiceLineItem;
import com.invoiceiq.validation.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaxMathValidationRule implements ValidationRule {

    private static final BigDecimal TOLERANCE = new BigDecimal("1.00");

    @Override
    public String getRuleCode() {
        return "TAX_MATH";
    }

    @Override
    public String getRuleName() {
        return "Tax Math & Split Verification";
    }

    @Override
    public ValidationResult validate(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount();
        if (total == null) {
            return ValidationResult.fail(
                    getRuleCode(), getRuleName(), "totalAmount",
                    "Invoice total amount is missing.",
                    Map.of("totalAmount", "NULL")
            );
        }

        BigDecimal subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        BigDecimal cgst = invoice.getCgstAmount() != null ? invoice.getCgstAmount() : BigDecimal.ZERO;
        BigDecimal sgst = invoice.getSgstAmount() != null ? invoice.getSgstAmount() : BigDecimal.ZERO;
        BigDecimal igst = invoice.getIgstAmount() != null ? invoice.getIgstAmount() : BigDecimal.ZERO;

        BigDecimal calculatedTotal = subtotal.add(cgst).add(sgst).add(igst);
        BigDecimal subtotalDiff = calculatedTotal.subtract(total).abs();

        if (subtotalDiff.compareTo(TOLERANCE) > 0) {
            Map<String, Object> details = new HashMap<>();
            details.put("subtotal", subtotal);
            details.put("cgst", cgst);
            details.put("sgst", sgst);
            details.put("igst", igst);
            details.put("calculatedTotal", calculatedTotal);
            details.put("invoiceTotal", total);
            details.put("difference", subtotalDiff);

            return ValidationResult.fail(
                    getRuleCode(), getRuleName(), "totalAmount",
                    String.format("Grand total (%s) does not match subtotal + taxes (%s). Difference: %s",
                            total, calculatedTotal, subtotalDiff),
                    details
            );
        }

        // Validate line items sum vs total
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            BigDecimal lineItemsSum = invoice.getLineItems().stream()
                    .map(item -> item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal lineDiff = lineItemsSum.subtract(total).abs();
            if (lineDiff.compareTo(TOLERANCE) > 0) {
                return ValidationResult.warn(
                        getRuleCode(), getRuleName(), "lineItems",
                        String.format("Sum of line items (%s) deviates from grand total (%s) by %s",
                                lineItemsSum, total, lineDiff),
                        Map.of("lineItemsSum", lineItemsSum, "invoiceTotal", total, "diff", lineDiff)
                );
            }
        }

        // Validate GST tax split against state codes (Intra-state vs Inter-state)
        String vendorGstin = invoice.getVendorGstin();
        String buyerGstin = invoice.getBuyerGstin();

        if (vendorGstin != null && vendorGstin.length() >= 2 && buyerGstin != null && buyerGstin.length() >= 2) {
            String vendorState = vendorGstin.substring(0, 2);
            String buyerState = buyerGstin.substring(0, 2);

            boolean isIntraState = vendorState.equals(buyerState);

            if (isIntraState) {
                // Intra-State: IGST should be zero; CGST & SGST should be equal
                if (igst.compareTo(BigDecimal.ZERO) > 0) {
                    return ValidationResult.warn(
                            getRuleCode(), getRuleName(), "igstAmount",
                            String.format("Intra-state supply (State code %s): IGST (%s) is charged instead of CGST/SGST.", vendorState, igst),
                            Map.of("vendorState", vendorState, "buyerState", buyerState, "igst", igst)
                    );
                }
                BigDecimal cgstSgstDiff = cgst.subtract(sgst).abs();
                if (cgstSgstDiff.compareTo(TOLERANCE) > 0) {
                    return ValidationResult.warn(
                            getRuleCode(), getRuleName(), "cgstAmount",
                            String.format("Intra-state supply: CGST (%s) and SGST (%s) should be equal.", cgst, sgst),
                            Map.of("cgst", cgst, "sgst", sgst, "diff", cgstSgstDiff)
                    );
                }
            } else {
                // Inter-State: CGST and SGST should be zero; IGST should be charged
                if (cgst.compareTo(BigDecimal.ZERO) > 0 || sgst.compareTo(BigDecimal.ZERO) > 0) {
                    return ValidationResult.warn(
                            getRuleCode(), getRuleName(), "cgstAmount",
                            String.format("Inter-state supply (Vendor %s -> Buyer %s): CGST/SGST is charged instead of IGST.", vendorState, buyerState),
                            Map.of("vendorState", vendorState, "buyerState", buyerState, "cgst", cgst, "sgst", sgst)
                    );
                }
            }
        }

        return ValidationResult.pass(getRuleCode(), getRuleName(), "All tax calculations, line item sums, and GST splits match accurately.");
    }
}
