package com.invoiceiq.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.minio")
@Getter
@Setter
public class MinioConfig {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin_secret";
    private String bucketName = "invoiceiq-invoices";
    private int presignedExpirySeconds = 900;

    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinIO client with endpoint: {}, bucket: {}", endpoint, bucketName);
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                log.info("Bucket '{}' does not exist. Creating it now.", bucketName);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                log.info("Bucket '{}' verified successfully.", bucketName);
            }
        } catch (Exception e) {
            log.warn("Could not verify or create MinIO bucket on startup. Will attempt lazily: {}", e.getMessage());
        }

        return client;
    }
}
