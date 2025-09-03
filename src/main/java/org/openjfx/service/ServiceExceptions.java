package org.openjfx.service;

/**
 * Typed exceptions for service layer with user-friendly messages.
 */
public class ServiceExceptions {
    public static class KeystoreLoadException extends Exception {
        public KeystoreLoadException(String message, Throwable cause) { super(message, cause); }
        public KeystoreLoadException(String message) { super(message); }
    }
    public static class ExportException extends Exception {
        public ExportException(String message, Throwable cause) { super(message, cause); }
        public ExportException(String message) { super(message); }
    }
    public static class CertificateLoadException extends Exception {
        public CertificateLoadException(String message, Throwable cause) { super(message, cause); }
        public CertificateLoadException(String message) { super(message); }
    }
}