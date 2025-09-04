package org.openjfx.service.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Locale;

/** Default JRE providers for JKS and PKCS12. */
public class SunJksPkcs12Strategy implements KeystoreProviderStrategy {
    @Override
    public boolean supports(File file) {
        String n = file.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".jks") || n.endsWith(".ks") || n.endsWith(".p12") || n.endsWith(".pfx");
    }

    @Override
    public String getType(File file) {
        String n = file.getName().toLowerCase(Locale.ROOT);
        return (n.endsWith(".p12") || n.endsWith(".pfx")) ? "PKCS12" : "JKS";
    }

    @Override
    public KeyStore load(File file, char[] password) throws Exception {
        String type = getType(file);
        try (FileInputStream fis = new FileInputStream(file)) {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(fis, (password != null && password.length > 0) ? password : null);
            return ks;
        }
    }
}
