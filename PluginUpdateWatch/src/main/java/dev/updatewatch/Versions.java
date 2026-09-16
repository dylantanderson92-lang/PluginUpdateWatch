package dev.updatewatch;

import java.math.BigInteger;
import java.util.Locale;

final class Versions {
    enum Status { CURRENT, UPDATE, DIFFERENT }
    static String normalize(String v) {
        return v.trim().toLowerCase(Locale.ROOT).replaceFirst("^v(?=\\d)", "").split("\\+", 2)[0];
    }
    static Status compare(String installed, String latest) {
        String a = normalize(installed), b = normalize(latest);
        if (a.equals(b)) return Status.CURRENT;
        if (!a.matches("\\d+(\\.\\d+)*") || !b.matches("\\d+(\\.\\d+)*")) return Status.DIFFERENT;
        String[] aa = a.split("\\."), bb = b.split("\\.");
        for (int i = 0; i < Math.max(aa.length, bb.length); i++) {
            int c = new BigInteger(i < aa.length ? aa[i] : "0").compareTo(new BigInteger(i < bb.length ? bb[i] : "0"));
            if (c != 0) return c < 0 ? Status.UPDATE : Status.CURRENT;
        }
        return Status.CURRENT;
    }
}
