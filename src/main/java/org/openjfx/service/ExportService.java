package org.openjfx.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.Base64;

public class ExportService {

    public void exportCertificatePem(Certificate cert, Path output) throws Exception {
        String pem = "-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(cert.getEncoded()) +
                "\n-----END CERTIFICATE-----\n";
        Files.writeString(output, pem, StandardCharsets.US_ASCII);
    }

    public void exportCertificateDer(Certificate cert, Path output) throws Exception {
        Files.write(output, cert.getEncoded());
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
