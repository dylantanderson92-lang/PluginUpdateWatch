package dev.updatewatch;

final class Compatibility {
    enum Status { BELOW_MINIMUM, TARGETED, UNVERIFIED }
    static Status classify(String version) {
        if (version.equals("1.21")) return Status.BELOW_MINIMUM;
        if (version.matches("1\\.21\\.[0-9]+")) return Integer.parseInt(version.substring(5)) >= 11 ? Status.TARGETED : Status.BELOW_MINIMUM;
        if (version.matches("1\\.[0-9]+(?:\\.[0-9]+)?")) return Integer.parseInt(version.split("\\.")[1]) < 21 ? Status.BELOW_MINIMUM : Status.UNVERIFIED;
        if (version.matches("26\\.[1-3](?:\\.[0-9]+)?")) return Status.TARGETED;
        return Status.UNVERIFIED;
    }
}
