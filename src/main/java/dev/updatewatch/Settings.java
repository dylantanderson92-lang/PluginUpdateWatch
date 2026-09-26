package dev.updatewatch;

import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;

record Settings(int connectMillis, int readMillis, int metadataSeconds, int downloadSeconds,
                long downloadBytes, int attempts, Path pluginsFolder, boolean debug) {
    static Settings defaults() { return new Settings(10000, 15000, 30, 120, 100L * 1024 * 1024, HttpTransport.MAX_RETRIES, Path.of("plugins"), false); }
    static boolean requireChecksum(YamlConfiguration config) {
        if (!config.contains("downloads.require-checksum")) return true;
        Object value = config.get("downloads.require-checksum");
        if (!(value instanceof Boolean required)) throw new IllegalArgumentException("downloads.require-checksum must be true or false");
        return required;
    }
    static Settings parse(YamlConfiguration config, Path defaultFolder) {
        String folder = config.getString("plugins-directory", "");
        return new Settings(number(config, "network.connect-timeout-seconds", 10, 1, 60) * 1000,
                number(config, "network.read-timeout-seconds", 15, 1, 120) * 1000,
                number(config, "network.metadata-timeout-seconds", 30, 1, 300),
                number(config, "downloads.timeout-seconds", 120, 1, 1800),
                number(config, "downloads.max-size-mib", 100, 1, 1024) * 1024L * 1024,
                number(config, "network.attempts", HttpTransport.MAX_RETRIES, 1, 5),
                folder == null || folder.isBlank() ? defaultFolder.toAbsolutePath().normalize() : Path.of(folder).toAbsolutePath().normalize(),
                config.getBoolean("debug", false));
    }
    private static int number(YamlConfiguration config, String key, int fallback, int min, int max) {
        if (!config.contains(key)) return fallback;
        Object value = config.get(key);
        if (!(value instanceof Number n) || n.doubleValue() != n.intValue() || n.intValue() < min || n.intValue() > max)
            throw new IllegalArgumentException(key + " must be an integer from " + min + " to " + max);
        return n.intValue();
    }
}
