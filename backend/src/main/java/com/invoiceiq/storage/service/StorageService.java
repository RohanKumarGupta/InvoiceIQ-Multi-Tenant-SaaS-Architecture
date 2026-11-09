package com.invoiceiq.storage.service;

import java.io.InputStream;

public interface StorageService {

    /**
     * Upload an input stream to object storage.
     */
    void upload(String objectKey, InputStream inputStream, long size, String contentType);

    /**
     * Get a temporary pre-signed URL to view / download the file.
     */
    String generatePresignedUrl(String objectKey, int expirySeconds);

    /**
     * Download the file as an input stream.
     */
    InputStream download(String objectKey);

    /**
     * Delete an object.
     */
    void delete(String objectKey);
}
