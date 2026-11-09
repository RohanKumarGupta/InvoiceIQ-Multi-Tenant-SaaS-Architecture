package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.invoice.repository.InvoiceRepository;
import com.invoiceiq.validation.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendorAnomalyValidationRule implements ValidationRule {

    private final InvoiceRepository invoiceRepository;
    private static final int MIN_HISTORICAL_SAMPLES = 2;

    @Override
    public String getRuleCode() {
        return "VENDOR_ANOMALY";
    }

    @Override
    public String getRuleName() {
        return "Vendor Historical Spend Anomaly Detection";
    }

    @Override
    public ValidationResult validate(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.pass(getRuleCode(), getRuleName(), "Amount is null or zero; anomaly check skipped.");
        }

        String vendorGstin = invoice.getVendorGstin() != null ? invoice.getVendorGstin() : "";
        String vendorName = invoice.getVendorName() != null ? invoice.getVendorName() : "";

        if (vendorGstin.isBlank() && vendorName.isBlank()) {
            return ValidationResult.pass(getRuleCode(), getRuleName(), "No vendor identified; anomaly check skipped.");
        }

        List<Invoice> history = invoiceRepository.findHistoricalInvoicesForVendor(
                invoice.getTenantId(), invoice.getId(), vendorGstin, vendorName);

        if (history.size() < MIN_HISTORICAL_SAMPLES) {
            return ValidationResult.pass(getRuleCode(), getRuleName(),
                    String.format("Insufficient historical data (%d past invoices found). Baseline requires at least %d.",
                            history.size(), MIN_HISTORICAL_SAMPLES));
        }

        // Calculate mean & variance
        double sum = 0.0;
        for (Invoice past : history) {
            sum += past.getTotalAmount().doubleValue();
        }
        double mean = sum / history.size();

        double varianceSum = 0.0;
        for (Invoice past : history) {
            varianceSum += Math.pow(past.getTotalAmount().doubleValue() - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / history.size());

        double currentVal = total.doubleValue();
        double ratio = mean > 0 ? (currentVal / mean) : 1.0;

        // Flag anomaly if total exceeds 3.0x average or > mean + 2.5 * stdDev
        if (ratio >= 3.0 || (stdDev > 0 && currentVal > (mean + 2.5 * stdDev))) {
            double percentIncrease = (ratio - 1.0) * 100.0;
            Map<String, Object> details = new HashMap<>();
            details.put("currentAmount", total);
            details.put("historicalAverage", BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP));
            details.put("historicalSamples", history.size());
            details.put("percentDeviation", BigDecimal.valueOf(percentIncrease).setScale(1, RoundingMode.HALF_UP));

            return ValidationResult.warn(
                    getRuleCode(), getRuleName(), "totalAmount",
                    String.format("High spend anomaly: Invoice amount %s is %.1f%% higher than historical average of %s across %d past invoices from this vendor.",
                            total, percentIncrease, BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP), history.size()),
                    details
            );
        }

        return ValidationResult.pass(getRuleCode(), getRuleName(),
                String.format("Invoice amount %s is consistent with vendor's historical average of %s (%d records).",
                        total, BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP), history.size()));
    }
}
