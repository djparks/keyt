package org.openjfx.service;

import org.openjfx.model.CertificateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

public class KeystoreService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeystoreService.class);

    private final java.util.List<org.openjfx.service.keystore.KeystoreProviderStrategy> strategies = java.util.List.of(
            new org.openjfx.service.keystore.SunJksPkcs12Strategy()
    );

    public KeyStore load(File file, char[] ksPassword) throws org.openjfx.service.ServiceExceptions.KeystoreLoadException {
        try {
            for (org.openjfx.service.keystore.KeystoreProviderStrategy s : strategies) {
                if (s.supports(file)) {
                    return s.load(file, ksPassword);
                }
            }
            // Fallback to default JKS
            try (FileInputStream fis = new FileInputStream(file)) {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(fis, (ksPassword != null && ksPassword.length > 0) ? ksPassword : null);
                return ks;
            }
        } catch (Exception e) {
            log.debug("Keystore load failed for {}", file, e);
            throw new org.openjfx.service.ServiceExceptions.KeystoreLoadException("Unable to load keystore: " + file.getName(), e);
        }
    }

    public List<CertificateInfo> listEntries(KeyStore ks) throws org.openjfx.service.ServiceExceptions.KeystoreLoadException {
        List<CertificateInfo> result = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        try {
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                String entryType = ks.isKeyEntry(alias) ? "Private Key" : (ks.isCertificateEntry(alias) ? "Trusted Certificate" : "Unknown");
                Certificate cert = ks.getCertificate(alias);
                String validFrom = "";
                String validUntil = "";
                String sigAlg = "";
                String serial = "";
                if (cert instanceof X509Certificate x509) {
                    validFrom = fmt.format(x509.getNotBefore());
                    validUntil = fmt.format(x509.getNotAfter());
                    sigAlg = x509.getSigAlgName();
                    serial = x509.getSerialNumber() != null ? x509.getSerialNumber().toString(16).toUpperCase(Locale.ROOT) : "";
                }
                result.add(new CertificateInfo(alias, entryType, validFrom, validUntil, sigAlg, serial));
            }
            return result;
        } catch (Exception e) {
            log.debug("List entries failed", e);
            throw new org.openjfx.service.ServiceExceptions.KeystoreLoadException("Unable to list entries", e);
        }
    }

    public Optional<Certificate> getCertificate(KeyStore ks, String alias) throws Exception {
        return Optional.ofNullable(ks.getCertificate(alias));
    }

    public KeyStore convertToPkcs12(KeyStore source, char[] ksPwd, char[] keyPwd) throws Exception {
        KeyStore p12 = KeyStore.getInstance("PKCS12");
        p12.load(null, null);
        char[] keyPassword = (keyPwd != null && keyPwd.length > 0) ? keyPwd : ((ksPwd != null) ? ksPwd : new char[0]);
        for (Enumeration<String> ealiases = source.aliases(); ealiases.hasMoreElements(); ) {
            String alias = ealiases.nextElement();
            if (source.isKeyEntry(alias)) {
                Key key = source.getKey(alias, keyPassword);
                Certificate[] chain = source.getCertificateChain(alias);
                if (chain == null) {
                    Certificate c = source.getCertificate(alias);
                    if (c != null) {
                        chain = new Certificate[]{c};
                    }
                }
                if (chain == null || key == null) {
                    throw new Exception("Missing key or certificate chain for alias: " + alias);
                }
                p12.setKeyEntry(alias, key, keyPassword, chain);
            } else if (source.isCertificateEntry(alias)) {
                Certificate cert = source.getCertificate(alias);
                if (cert != null) {
                    p12.setCertificateEntry(alias, cert);
                }
            }
        }
        return p12;
    }

    public List<CertificateInfo> loadCertificates(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            return mapCertificates(certs, file.getName());
        }
    }

    private List<CertificateInfo> mapCertificates(Collection<? extends Certificate> certs, String fileName) {
        List<CertificateInfo> list = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        int idx = 1;
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate x509) {
                String alias = x509.getSubjectX500Principal() != null ? x509.getSubjectX500Principal().getName() : (fileName + "#" + idx);
                String validFrom = fmt.format(x509.getNotBefore());
                String validUntil = fmt.format(x509.getNotAfter());
                String sigAlg = x509.getSigAlgName();
                String serial = x509.getSerialNumber() != null ? x509.getSerialNumber().toString(16).toUpperCase(Locale.ROOT) : "";
                list.add(new CertificateInfo(alias, "Certificate", validFrom, validUntil, sigAlg, serial));
                idx++;
            }
        }
        return list;
    }
}