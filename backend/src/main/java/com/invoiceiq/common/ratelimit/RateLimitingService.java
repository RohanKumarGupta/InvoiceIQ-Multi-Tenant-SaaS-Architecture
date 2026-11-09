package com.invoiceiq.common.ratelimit;

import com.invoiceiq.common.exception.ApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitingService {

    // In-memory bucket registry per tenant; ready to be backed by Redis in clustered deployments
    private final Map<UUID, Bucket> uploadBuckets = new ConcurrentHashMap<>();

    // Standard limit: 30 invoice uploads per minute per tenant
    private static final int UPLOAD_CAPACITY = 30;
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    public void checkUploadRateLimit(UUID tenantId) {
        Bucket bucket = uploadBuckets.computeIfAbsent(tenantId, this::createNewUploadBucket);

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for tenant {} on invoice upload endpoint", tenantId);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Upload rate limit exceeded (maximum 30 uploads per minute). Please try again shortly.");
        }
    }

    private Bucket createNewUploadBucket(UUID tenantId) {
        Bandwidth limit = Bandwidth.classic(
                UPLOAD_CAPACITY,
                Refill.greedy(UPLOAD_CAPACITY, REFILL_DURATION)
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
