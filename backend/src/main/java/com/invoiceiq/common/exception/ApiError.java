package com.invoiceiq.common.exception;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    String correlationId,
    Map<String, String> fieldErrors
) {}
