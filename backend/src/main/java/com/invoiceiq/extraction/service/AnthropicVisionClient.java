package com.invoiceiq.extraction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceiq.common.exception.ApiException;
import com.invoiceiq.extraction.config.AiExtractionConfig;
import com.invoiceiq.extraction.prompt.ExtractionPromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicVisionClient {

    private final AiExtractionConfig config;
    private final ObjectMapper objectMapper;

    public record VisionResponse(
            String content,
            int promptTokens,
            int completionTokens,
            long executionTimeMs,
            boolean isSynthetic
    ) {}

    public VisionResponse callClaudeVision(byte[] fileBytes, String contentType) {
        long startTime = System.currentTimeMillis();

        if (!config.isConfigured()) {
            log.warn("ANTHROPIC_API_KEY is not configured or is a placeholder. Emulating high-fidelity GST vision response for local development.");
            long latency = Math.max(120, System.currentTimeMillis() - startTime);
            return new VisionResponse(generateSyntheticGstExtraction(), 1450, 420, latency, true);
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        RestClient restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        Map<String, Object> requestPayload = buildAnthropicRequest(base64Data, contentType);

        int maxRetries = Math.max(1, config.getMaxRetries());
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Sending Claude vision extraction request to Anthropic (attempt {}/{}, model: {})",
                        attempt, maxRetries, config.getModel());

                String rawResponse = restClient.post()
                        .uri("/v1/messages")
                        .header("x-api-key", config.getApiKey())
                        .header("anthropic-version", "2023-06-01")
                        .header("anthropic-beta", "pdfs-2024-09-25")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(requestPayload)
                        .retrieve()
                        .body(String.class);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Received Claude response in {}ms", duration);

                return parseAnthropicResponse(rawResponse, duration);

            } catch (Exception e) {
                lastException = e;
                log.warn("Anthropic API call attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        long backoffMs = (long) Math.pow(2, attempt) * 1000;
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All {} attempts to call Anthropic Vision failed: {}", maxRetries, lastException != null ? lastException.getMessage() : "Unknown");
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI vision service is currently unavailable after retries: " +
                (lastException != null ? lastException.getMessage() : ""));
    }

    private Map<String, Object> buildAnthropicRequest(String base64Data, String contentType) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", config.getModel());
        request.put("max_tokens", 4096);
        request.put("system", ExtractionPromptTemplates.SYSTEM_PROMPT);

        Map<String, Object> mediaContent = new HashMap<>();
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            mediaContent.put("type", "document");
            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", "application/pdf");
            source.put("data", base64Data);
            mediaContent.put("source", source);
        } else {
            mediaContent.put("type", "image");
            Map<String, Object> source = new HashMap<>();
            source.put("type", "base64");
            source.put("media_type", contentType != null ? contentType : "image/jpeg");
            source.put("data", base64Data);
            mediaContent.put("source", source);
        }

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", ExtractionPromptTemplates.USER_PROMPT);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", List.of(mediaContent, textContent));

        request.put("messages", List.of(userMessage));
        return request;
    }

    private VisionResponse parseAnthropicResponse(String rawJson, long executionTimeMs) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            StringBuilder textBuilder = new StringBuilder();

            JsonNode contentArray = root.path("content");
            if (contentArray.isArray()) {
                for (JsonNode block : contentArray) {
                    if ("text".equals(block.path("type").asText())) {
                        textBuilder.append(block.path("text").asText());
                    }
                }
            }

            JsonNode usageNode = root.path("usage");
            int promptTokens = usageNode.path("input_tokens").asInt(0);
            int completionTokens = usageNode.path("output_tokens").asInt(0);

            return new VisionResponse(textBuilder.toString(), promptTokens, completionTokens, executionTimeMs, false);
        } catch (Exception e) {
            log.error("Failed to parse Anthropic JSON response envelope: {}", e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse AI response envelope");
        }
    }

    private String generateSyntheticGstExtraction() {
        return """
                {
                  "invoiceNumber": "INV-2024-0089",
                  "invoiceNumberConfidence": 0.98,
                  "invoiceDate": "2024-10-15",
                  "invoiceDateConfidence": 0.99,
                  "dueDate": "2024-11-14",
                  "dueDateConfidence": 0.95,
                  "currency": "INR",
                  "vendorName": "Infotech Solutions Private Limited",
                  "vendorNameConfidence": 0.99,
                  "vendorGstin": "29AABCU9603R1ZM",
                  "vendorGstinConfidence": 0.99,
                  "buyerGstin": "29XYZAB5678C1Z2",
                  "buyerGstinConfidence": 0.97,
                  "subtotal": 50000.00,
                  "subtotalConfidence": 0.98,
                  "cgstAmount": 4500.00,
                  "cgstConfidence": 0.98,
                  "sgstAmount": 4500.00,
                  "sgstConfidence": 0.98,
                  "igstAmount": 0.00,
                  "igstConfidence": 0.99,
                  "totalAmount": 59000.00,
                  "totalAmountConfidence": 0.99,
                  "overallConfidence": 0.98,
                  "lineItems": [
                    {
                      "lineNumber": 1,
                      "description": "Cloud Infrastructure Consulting & Setup",
                      "hsnSac": "998313",
                      "quantity": 1.0,
                      "unitPrice": 35000.00,
                      "taxRate": 18.00,
                      "cgstAmount": 3150.00,
                      "sgstAmount": 3150.00,
                      "igstAmount": 0.00,
                      "totalAmount": 41300.00,
                      "confidence": 0.98
                    },
                    {
                      "lineNumber": 2,
                      "description": "DevOps Maintenance Support (Quarterly)",
                      "hsnSac": "998314",
                      "quantity": 1.0,
                      "unitPrice": 15000.00,
                      "taxRate": 18.00,
                      "cgstAmount": 1350.00,
                      "sgstAmount": 1350.00,
                      "igstAmount": 0.00,
                      "totalAmount": 17700.00,
                      "confidence": 0.97
                    }
                  ]
                }
                """;
    }
}
