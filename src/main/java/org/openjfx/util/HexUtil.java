package org.openjfx.util;

import java.util.Locale;

public final class HexUtil {
    private HexUtil() {}

    public static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte value : b) sb.append(String.format(Locale.ROOT, "%02X", value));
        return sb.toString();
    }

    /** keytool-style: colon-separated uppercase hex pairs */
    public static String toColonHex(byte[] b) {
        if (b == null || b.length == 0) return "";
        StringBuilder sb = new StringBuilder(b.length * 3 - 1);
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format(Locale.ROOT, "%02X", b[i]));
        }
        return sb.toString();
    }
}
