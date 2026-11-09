package com.invoiceiq.extraction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "app.anthropic")
@Getter
@Setter
public class AiExtractionConfig {

    private String apiKey = "";
    private String model = "claude-3-5-sonnet-20241022";
    private String baseUrl = "https://api.anthropic.com";
    private int timeoutSeconds = 60;
    private int maxRetries = 3;

    // Pricing estimation: Claude 3.5 Sonnet ($3.00/MTok in, $15.00/MTok out)
    private BigDecimal inputTokenCostPerMillion = new BigDecimal("3.00");
    private BigDecimal outputTokenCostPerMillion = new BigDecimal("15.00");

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("sk-ant-your-key");
    }
}
