package com.invoiceiq.validation.model;

import lombok.Builder;

import java.util.Map;

@Builder
public record ValidationResult(
        String ruleCode,
        String ruleName,
        ValidationStatus status,
        String field,
        String message,
        Map<String, Object> details
) {
    public static ValidationResult pass(String ruleCode, String ruleName, String message) {
        return ValidationResult.builder()
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .status(ValidationStatus.PASS)
                .message(message)
                .details(Map.of())
                .build();
    }

    public static ValidationResult warn(String ruleCode, String ruleName, String field, String message, Map<String, Object> details) {
        return ValidationResult.builder()
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .status(ValidationStatus.WARN)
                .field(field)
                .message(message)
                .details(details != null ? details : Map.of())
                .build();
    }

    public static ValidationResult fail(String ruleCode, String ruleName, String field, String message, Map<String, Object> details) {
        return ValidationResult.builder()
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .status(ValidationStatus.FAIL)
                .field(field)
                .message(message)
                .details(details != null ? details : Map.of())
                .build();
    }
}
