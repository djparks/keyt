package org.openjfx.util;

import org.openjfx.model.CertificateInfo;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Utility methods for working with X.509 certificates.
 */
public final class CertificateUtil {

    private CertificateUtil() {
    }

    /**
     * Maps a collection of Certificates to CertificateInfo entries. Only X509 certificates are mapped.
     * This method mirrors the previous implementations in CertificateService and KeystoreService.
     *
     * @param certs    certificates to map
     * @param fileName fallback source name used to generate default aliases
     * @return list of mapped CertificateInfo
     */
    public static List<CertificateInfo> mapCertificates(Collection<? extends Certificate> certs, String fileName) {
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
