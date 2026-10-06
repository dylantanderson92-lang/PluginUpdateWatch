package dev.updatewatch;

import java.util.Locale;

enum ReleaseType {
    STABLE("stable", false), BETA("beta", true), ALPHA("alpha", true),
    PRERELEASE("prerelease", true), RELEASE_CANDIDATE("release candidate (version label)", true),
    DEVELOPMENT("development/snapshot (version label)", true), UNKNOWN("unknown", true);
    final String label;
    final boolean warning;
    ReleaseType(String label, boolean warning) { this.label = label; this.warning = warning; }
    static ReleaseType modrinth(String type) {
        return switch (type) { case "release" -> STABLE; case "beta" -> BETA; case "alpha" -> ALPHA; default -> UNKNOWN; };
    }
    static ReleaseType fromLabel(String version) {
        String label = version.toLowerCase(Locale.ROOT);
        if (label.matches(".*(?:^|[^a-z])(?:snapshot|dev|nightly)(?:[^a-z]|$).*")) return DEVELOPMENT;
        if (label.matches(".*(?:^|[^a-z])alpha(?:[^a-z]|$).*")) return ALPHA;
        if (label.matches(".*(?:^|[^a-z])beta(?:[^a-z]|$).*")) return BETA;
        if (label.matches(".*(?:^|[^a-z])rc(?:[^a-z]|$).*")) return RELEASE_CANDIDATE;
        return UNKNOWN;
    }
    String message() {
        return (warning ? "[WARNING]" : "[INFO]") + " Release type: " + label
                + (warning ? "; this may be a test/development build. Review the publisher's notes before installing." : "");
    }
}
