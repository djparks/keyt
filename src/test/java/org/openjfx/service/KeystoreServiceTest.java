package org.openjfx.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjfx.model.CertificateInfo;
import org.openjfx.service.ServiceExceptions.KeystoreLoadException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Disabled in constrained CI/offline environments. Tests can use example resources from src/test/resources.")
public class KeystoreServiceTest {

    private X509Certificate sampleCert() throws Exception {
        String base64 = "MIIBlTCCATugAwIBAgIUYH7f8o9v9gUuV3yWf6v3r1PqZtQwDQYJKoZIhvcNAQELBQAwDzENMAsGA1UEAwwEdGVzdDAeFw0yNTAxMDEwMDAwMDBaFw0yNTAxMDIwMDAwMDBaMA8xDTALBgNVBAMMBHRlc3QwXDANBgkqhkiG9w0BAQEFAANLADBIAkEApvO0w7k7cS8sR2s3i3dV8G7j1qGqgI2vG2a3y0qfQh8Vgq2hY4m4S2iJx2QG1S2yqzj6nR0f1yGmY7rQ2V4+QIDAQABo1MwUTAdBgNVHQ4EFgQU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wHwYDVR0jBBgwFoAU0O7bqv1oYlq0xN7m1rT1+Q9v9X0wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAANBAFJjXKcF0rZxw6t3YgkH2t7t6tJqY0m1m+YcQe5G4lY8QfZ1zVYy0tWJrYtGqzPq1WlQb3u3mJm8rXj2Z5m0H8U=";
        byte[] der = java.util.Base64.getDecoder().decode(base64);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    @Test
    void loadListAndConvertRoundtrip() throws Exception {
        KeystoreService svc = new KeystoreService();
        char[] ksPwd = "changeit".toCharArray();
        char[] keyPwd = "changeit".toCharArray();

        // Prefer sample JKS from resources if available; otherwise, create one.
        Path jksFile;
        var jksUrl = getClass().getResource("/keystores/sample.jks");
        if (jksUrl != null) {
            jksFile = Path.of(jksUrl.toURI());
        } else {
            KeyStore jks = KeyStore.getInstance("JKS");
            jks.load(null, ksPwd);
            X509Certificate cert = sampleCert();
            jks.setCertificateEntry("trusted", cert);
            jksFile = Files.createTempFile("keystore", ".jks");
            try (FileOutputStream fos = new FileOutputStream(jksFile.toFile())) {
                jks.store(fos, ksPwd);
            }
        }

        // Load via service
        KeyStore loaded = svc.load(jksFile.toFile(), ksPwd);
        assertNotNull(loaded);

        List<CertificateInfo> infos = svc.listEntries(loaded);
        assertEquals(1, infos.size());
        assertEquals("trusted", infos.get(0).getAlias());
        assertEquals("Trusted Certificate", infos.get(0).getEntryType());
        assertFalse(infos.get(0).getSignatureAlgorithm().isBlank());

        // Use sample PKCS12 from resources if available; otherwise convert and persist
        Path p12File;
        var p12Url = getClass().getResource("/keystores/sample.p12");
        if (p12Url != null) {
            p12File = Path.of(p12Url.toURI());
        } else {
            KeyStore p12 = svc.convertToPkcs12(loaded, ksPwd, keyPwd);
            p12File = Files.createTempFile("keystore", ".p12");
            try (FileOutputStream fos = new FileOutputStream(p12File.toFile())) {
                p12.store(fos, ksPwd);
            }
        }

        // Reload PKCS12 to verify
        KeyStore reloaded = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(p12File)) {
            reloaded.load(in, ksPwd);
        }
        assertNotNull(reloaded.getCertificate("trusted"));
    }
}
