package com.invoiceiq.validation.rule;

import com.invoiceiq.invoice.model.Invoice;
import com.invoiceiq.validation.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class GstinChecksumValidationRule implements ValidationRule {

    private static final String GSTIN_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
    private static final Pattern GSTIN_PATTERN = Pattern.compile(GSTIN_REGEX);
    private static final String CONVERSION_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Override
    public String getRuleCode() {
        return "GSTIN_CHECKSUM";
    }

    @Override
    public String getRuleName() {
        return "GSTIN Format & Checksum Verification";
    }

    @Override
    public ValidationResult validate(Invoice invoice) {
        String vendorGstin = invoice.getVendorGstin();
        if (vendorGstin == null || vendorGstin.trim().isEmpty()) {
            return ValidationResult.warn(
                    getRuleCode(), getRuleName(), "vendorGstin",
                    "Vendor GSTIN is missing. Input Tax Credit (ITC) may not be claimable without a valid GSTIN.",
                    Map.of("vendorGstin", "MISSING")
            );
        }

        vendorGstin = vendorGstin.trim().toUpperCase();

        if (!GSTIN_PATTERN.matcher(vendorGstin).matches()) {
            return ValidationResult.fail(
                    getRuleCode(), getRuleName(), "vendorGstin",
                    String.format("Vendor GSTIN '%s' has an invalid Indian GSTIN structure.", vendorGstin),
                    Map.of("providedGstin", vendorGstin, "expectedFormat", "22AAAAA0000A1Z5")
            );
        }

        // Modulo 36 Luhn Checksum verification
        boolean checksumValid = verifyGstinChecksum(vendorGstin);
        if (!checksumValid) {
            return ValidationResult.fail(
                    getRuleCode(), getRuleName(), "vendorGstin",
                    String.format("Vendor GSTIN '%s' failed modulo-36 checksum verification.", vendorGstin),
                    Map.of("providedGstin", vendorGstin, "checksumValid", false)
            );
        }

        // Buyer GSTIN validation if present
        String buyerGstin = invoice.getBuyerGstin();
        if (buyerGstin != null && !buyerGstin.trim().isEmpty()) {
            buyerGstin = buyerGstin.trim().toUpperCase();
            if (!GSTIN_PATTERN.matcher(buyerGstin).matches() || !verifyGstinChecksum(buyerGstin)) {
                return ValidationResult.warn(
                        getRuleCode(), getRuleName(), "buyerGstin",
                        String.format("Buyer GSTIN '%s' is present but failed checksum or format validation.", buyerGstin),
                        Map.of("buyerGstin", buyerGstin)
                );
            }
        }

        Map<String, Object> details = new HashMap<>();
        details.put("vendorGstin", vendorGstin);
        details.put("stateCode", vendorGstin.substring(0, 2));
        details.put("pan", vendorGstin.substring(2, 12));
        details.put("checksumValid", true);

        return ValidationResult.pass(getRuleCode(), getRuleName(), "Vendor GSTIN verified successfully with valid checksum.");
    }

    private boolean verifyGstinChecksum(String gstin) {
        if (gstin == null || gstin.length() != 15) return false;

        int factor = 2;
        int sum = 0;

        for (int i = 13; i >= 0; i--) {
            int codePoint = CONVERSION_CHARS.indexOf(gstin.charAt(i));
            if (codePoint == -1) return false;

            int digit = factor * codePoint;
            factor = (factor == 2) ? 1 : 2;

            digit = (digit / 36) + (digit % 36);
            sum += digit;
        }

        int remainder = sum % 36;
        int checkCodePoint = (36 - remainder) % 36;
        char expectedChar = CONVERSION_CHARS.charAt(checkCodePoint);

        return gstin.charAt(14) == expectedChar;
    }
}
