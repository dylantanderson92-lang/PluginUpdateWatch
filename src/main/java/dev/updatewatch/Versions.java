package dev.updatewatch;

import java.math.BigInteger;
import java.util.Locale;

final class Versions {
    enum Status { CURRENT, UPDATE, DIFFERENT, UNKNOWN }
    static String normalize(String v) {
        String value = v.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^(?:paper|spigot|bukkit)[-_ ](?=v?\\d)", "")
                .replaceFirst("[-_ ](?:paper|spigot|bukkit)$", "")
                .replaceFirst("^v(?=\\d)", "");
        // Plan publishes "5.8 build 3638", "5.8+build.3638" and "5.8.3638" for one build.
        var numberedBuild = java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d+)*)(?:[ +_-]+build[ ._-]+)(\\d+)$").matcher(value);
        if (numberedBuild.matches()) return numberedBuild.group(1) + "." + numberedBuild.group(2);
        String base = value.split("\\+", 2)[0];
        // Numeric SemVer build metadata does not order versions; custom dev build IDs may matter.
        return base.matches("\\d+(\\.\\d+)*") ? base : value;
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
