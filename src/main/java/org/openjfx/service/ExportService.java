package org.openjfx.service;

import org.openjfx.service.ServiceExceptions.ExportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.Base64;

public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    public void exportCertificatePem(Certificate cert, Path output) throws ExportException {
        try {
            String pem = "-----BEGIN CERTIFICATE-----\n" +
                    Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(cert.getEncoded()) +
                    "\n-----END CERTIFICATE-----\n";
            Files.writeString(output, pem, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            log.debug("Export PEM failed to {}", output, e);
            throw new ExportException("Failed to export certificate to PEM", e);
        }
    }

    public void exportCertificateDer(Certificate cert, Path output) throws ExportException {
        try {
            Files.write(output, cert.getEncoded());
        } catch (Exception e) {
            log.debug("Export DER failed to {}", output, e);
            throw new ExportException("Failed to export certificate to DER", e);
        }
    }

    /**
     * Basic filename sanitization for aliases to safe-ish filenames.
     */
    public String sanitizeAliasForFilename(String alias) {
        if (alias == null || alias.isBlank()) return "certificate";
        String s = alias.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (s.length() > 100) s = s.substring(0, 100);
        return s;
    }
}
