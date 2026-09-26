package dev.updatewatch;

import java.io.IOException;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;

final class ConfigManager {
    static YamlConfiguration parse(String contents) throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString(contents); return yaml;
    }
    /** Compare the user's original file immediately before replacing it atomically. */
    static void save(Path path, String expected, String replacement) throws IOException {
        Path temp = Files.createTempFile(path.getParent(), ".config-", ".tmp");
        try {
            Files.writeString(temp, replacement);
            if (!Files.readString(path).equals(expected)) throw new IOException("Config changed during scan; run /pu reload");
            // Do not fall back to an overwrite that could leave a partial config after a crash.
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
