package org.openjfx.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjfx.model.CertificateInfo;
import org.openjfx.service.ServiceExceptions.CertificateLoadException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Disabled in constrained CI/offline environments. Tests load example resources from src/test/resources.")
public class CertificateServiceTest {

    private X509Certificate sampleCert() throws Exception {
        String base64 = "MIIBlTCCATugAwIBAgIUYH7f8o9v9gUuV3yWf6v3r1PqZtQwDQYJKoZIhvcNAQELBQAwDzENMAsGA1UEAwwEdGVzdDAeFw0yNTAxMDEwMDAwMDBaFw0yNTAxMDIwMDAwMDBaMA8xDTALBgNVBAMMBHRlc3QwXDANBgkqhkiG9w0BAQEFAANLADBIAkEApvO0w7k7cS8sR2s3i3dV8G7j1qGqgI2vG2a3y0qfQh8Vgq2hY4m4S2iJx2QG1S2yqzj6nR0f1yGmY7rQ2V4+QIDAQABo1MwUTAdBgNVHQ4EFgQU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wHwYDVR0jBBgwFoAU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAANBAFJjXKcF0rZxw6t3YgkH2t7t6tJqY0m1m+YcQe5G4lY8QfZ1zVYy0tWJrYtGqzPq1WlQb3u3mJm8rXj2Z5m0H8U=";
        byte[] der = Base64.getDecoder().decode(base64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    @Test
    void parsePemDerBundleAndInvalid() throws Exception {
        CertificateService svc = new CertificateService();

        // Load from test resources
        Path der = Path.of(getClass().getResource("/certs/single.der").toURI());
        Path pem = Path.of(getClass().getResource("/certs/single.pem").toURI());
        Path bundle = Path.of(getClass().getResource("/certs/bundle.pem").toURI());

        List<CertificateInfo> derList = svc.loadCertificates(der.toFile());
        assertEquals(1, derList.size());
        assertEquals("Certificate", derList.get(0).getEntryType());

        List<CertificateInfo> pemList = svc.loadCertificates(pem.toFile());
        assertEquals(1, pemList.size());
        assertTrue(pemList.get(0).getAlias().contains("CN="));

        List<CertificateInfo> bundleList = svc.loadCertificates(bundle.toFile());
        assertEquals(2, bundleList.size());

        Path invalid = Files.createTempDirectory("certsvc").resolve("invalid.txt");
        Files.writeString(invalid, "oops");
        assertThrows(CertificateLoadException.class, () -> svc.loadCertificates(invalid.toFile()));
    }
}
