package com.invoiceiq.invoice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

public interface VirusScanService {
    /**
     * Inspect file bytes for viruses or malicious payloads.
     * @return true if clean, false if infected.
     */
    boolean isClean(byte[] fileBytes, String fileName);
}

@Slf4j
@Service
class DefaultVirusScanService implements VirusScanService {

    @Override
    public boolean isClean(byte[] fileBytes, String fileName) {
        // Production hook: Can delegate to ClamAV daemon via ClamavClient or external ICAP service
        log.debug("Virus scanning hook invoked for file: {} ({} bytes). Verification: CLEAN (placeholder)",
                fileName, fileBytes.length);
        return true;
    }
}
