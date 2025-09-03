package org.openjfx.service.keystore;

import java.io.File;
import java.security.KeyStore;

/**
 * Strategy interface to isolate provider-specific KeyStore handling.
 * Minimal surface for current app needs.
 */
public interface KeystoreProviderStrategy {
    /** Quick check based on filename/heuristics */
    boolean supports(File file);

    /** Returns a human-readable keystore type label (e.g., JKS, PKCS12). */
    String getType(File file);

    /** Load a keystore from the file using this strategy/provider. */
    KeyStore load(File file, char[] password) throws Exception;
}
