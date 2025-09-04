package org.openjfx.service;

import org.openjfx.model.CertificateInfo;
import org.openjfx.service.ServiceExceptions.CertificateLoadException;
import org.openjfx.util.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.List;

public class CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    /**
     * Load one or more X.509 certificates from a file (PEM/DER/PKCS7 bundle) and map them to CertificateInfo.
     */
    public List<CertificateInfo> loadCertificates(File file) throws CertificateLoadException {
        try (FileInputStream fis = new FileInputStream(file)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // CertificateFactory.generateCertificates handles PEM bundles and PKCS7 (DER or PEM) automatically
            Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            return CertificateUtil.mapCertificates(certs, file.getName());
        } catch (Exception e) {
            log.debug("Certificate load failed for {}", file, e);
            throw new CertificateLoadException("Unable to load certificate(s): " + file.getName(), e);
        }
    }
}
