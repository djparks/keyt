package org.openjfx.service;

import org.openjfx.model.CertificateInfo;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class CertificateService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CertificateService.class);

    /**
     * Load one or more X.509 certificates from a file (PEM/DER/PKCS7 bundle) and map them to CertificateInfo.
     */
    public List<CertificateInfo> loadCertificates(File file) throws org.openjfx.service.ServiceExceptions.CertificateLoadException {
        String lower = file.getName().toLowerCase(java.util.Locale.ROOT);
        try (FileInputStream fis = new FileInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // CertificateFactory.generateCertificates handles PEM bundles and PKCS7 (DER or PEM) automatically
            Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            return mapCertificates(certs, file.getName());
        } catch (Exception e) {
            log.debug("Certificate load failed for {}", file, e);
            throw new org.openjfx.service.ServiceExceptions.CertificateLoadException("Unable to load certificate(s): " + file.getName(), e);
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
