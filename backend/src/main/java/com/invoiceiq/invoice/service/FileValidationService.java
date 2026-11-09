package com.invoiceiq.invoice.service;

import com.invoiceiq.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class FileValidationService {

    @Value("${app.upload.max-file-size:10485760}")
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".pdf", ".png", ".jpg", ".jpeg");

    // Magic numbers
    private static final byte[] PDF_HEADER = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // 0x89 P N G
    private static final byte[] JPEG_HEADER = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public String validateAndDetectContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required and cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    String.format("File size (%d bytes) exceeds maximum allowed limit (%d bytes)", file.getSize(), maxFileSize));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Original file name must not be blank");
        }

        String lowerName = originalName.toLowerCase();
        boolean hasValidExt = ALLOWED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!hasValidExt) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unsupported file extension. Allowed extensions are: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Validate Magic Bytes
        byte[] header = new byte[8];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            if (read < 4) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "File is too small or corrupted");
            }
        } catch (IOException e) {
            log.error("Failed to read header of uploaded file {}: {}", originalName, e.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to inspect file headers");
        }

        if (matches(header, PDF_HEADER)) {
            return "application/pdf";
        } else if (matches(header, PNG_HEADER)) {
            return "image/png";
        } else if (matches(header, JPEG_HEADER)) {
            return "image/jpeg";
        } else {
            log.warn("Magic byte mismatch for file {}. Header bytes: {}", originalName, Arrays.toString(header));
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid file format: file header does not match genuine PDF or JPEG/PNG image data");
        }
    }

    private boolean matches(byte[] data, byte[] magic) {
        if (data.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }
}
