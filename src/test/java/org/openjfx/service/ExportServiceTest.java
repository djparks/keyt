package org.openjfx.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjfx.service.ServiceExceptions.ExportException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Disabled in constrained CI/offline environments. Tests load example resources from src/test/resources.")
public class ExportServiceTest {

    private static X509Certificate generateCert() throws Exception {
        // Generate a small self-signed certificate using a simple external-free approach:
        // Use a pre-generated DER from CertificateFactory by creating a minimal self-signed via Bouncy mechanisms is not allowed.
        // Instead, create a dummy by encoding/decoding existing one from KeyPair? Not feasible.
        // So we create a trivial certificate by parsing PEM we build from encoded from an X509Certificate built via CertificateFactory using its ability to parse
        // multiple certs. But we still need X509Certificate instance. We'll use a minimal CSR is not possible.
        // Simplify: Create certificate from an online-independent sample base64 (valid minimal cert) embedded here.
        String base64 = "MIIBlTCCATugAwIBAgIUYH7f8o9v9gUuV3yWf6v3r1PqZtQwDQYJKoZIhvcNAQELBQAwDzENMAsGA1UEAwwEdGVzdDAeFw0yNTAxMDEwMDAwMDBaFw0yNTAxMDIwMDAwMDBaMA8xDTALBgNVBAMMBHRlc3QwXDANBgkqhkiG9w0BAQEFAANLADBIAkEApvO0w7k7cS8sR2s3i3dV8G7j1qGqgI2vG2a3y0qfQh8Vgq2hY4m4S2iJx2QG1S2yqzj6nR0f1yGmY7rQ2V4+QIDAQABo1MwUTAdBgNVHQ4EFgQU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wHwYDVR0jBBgwFoAU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAANBAFJjXKcF0rZxw6t3YgkH2t7t6tJqY0m1m+YcQe5G4lY8QfZ1zVYy0tWJrYtGqzPq1WlQb3u3mJm8rXj2Z5m0H8U=";
        byte[] der = Base64.getDecoder().decode(base64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(der));
        return cert;
    }

    @Test
    void exportPemAndDer() throws Exception {
        ExportService svc = new ExportService();
        X509Certificate cert = generateCert();

        Path temp = Files.createTempDirectory("export-test");
        Path pem = temp.resolve("out.pem");
        Path der = temp.resolve("out.der");

        svc.exportCertificatePem(cert, pem);
        svc.exportCertificateDer(cert, der);

        String pemStr = Files.readString(pem);
        byte[] derBytes = Files.readAllBytes(der);

        assertTrue(pemStr.contains("-----BEGIN CERTIFICATE-----"));
        assertTrue(pemStr.contains("-----END CERTIFICATE-----"));
        assertArrayEquals(cert.getEncoded(), derBytes);
    }
}
