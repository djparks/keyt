package org.openjfx.model;

import java.util.Objects;

public class CertificateInfo {
    private final String alias;
    private final String entryType;
    private final String validFrom;
    private final String validUntil;
    private final String signatureAlgorithm;
    private final String serialNumber;

    public CertificateInfo(String alias, String entryType, String validFrom, String validUntil, String signatureAlgorithm, String serialNumber) {
        this.alias = alias;
        this.entryType = entryType;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.signatureAlgorithm = signatureAlgorithm;
        this.serialNumber = serialNumber;
    }

    public String getAlias() { return alias; }
    public String getEntryType() { return entryType; }
    public String getValidFrom() { return validFrom; }
    public String getValidUntil() { return validUntil; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public String getSerialNumber() { return serialNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CertificateInfo)) return false;
        CertificateInfo that = (CertificateInfo) o;
        return Objects.equals(alias, that.alias) && Objects.equals(entryType, that.entryType) && Objects.equals(validFrom, that.validFrom) && Objects.equals(validUntil, that.validUntil) && Objects.equals(signatureAlgorithm, that.signatureAlgorithm) && Objects.equals(serialNumber, that.serialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, entryType, validFrom, validUntil, signatureAlgorithm, serialNumber);
    }
}