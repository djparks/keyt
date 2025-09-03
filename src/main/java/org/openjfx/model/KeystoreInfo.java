package org.openjfx.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class KeystoreInfo {
    private final String type; // JKS or PKCS12
    private final Path path;   // may be null for in-memory
    private final int entryCount;

    // Optionally, we could hold lightweight entry aliases for quick display/summary
    private final List<String> aliases; // may be empty

    public KeystoreInfo(String type, Path path, int entryCount, List<String> aliases) {
        this.type = type;
        this.path = path;
        this.entryCount = entryCount;
        this.aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    public String getType() { return type; }
    public Path getPath() { return path; }
    public int getEntryCount() { return entryCount; }
    public List<String> getAliases() { return aliases; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeystoreInfo)) return false;
        KeystoreInfo that = (KeystoreInfo) o;
        return entryCount == that.entryCount && Objects.equals(type, that.type) && Objects.equals(path, that.path) && Objects.equals(aliases, that.aliases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, path, entryCount, aliases);
    }

    @Override
    public String toString() {
        return "KeystoreInfo{" +
                "type='" + type + '\'' +
                ", path=" + path +
                ", entryCount=" + entryCount +
                ", aliases=" + aliases +
                '}';
    }
}
