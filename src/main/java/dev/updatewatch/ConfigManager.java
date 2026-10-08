package dev.updatewatch;

import java.io.IOException;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;

final class ConfigManager {
    static YamlConfiguration parse(String contents) throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString(contents); return yaml;
    }
    /** Format only Bukkit's serialized YAML; nested values and block scalars keep their indentation. */
    static String serialize(YamlConfiguration yaml) {
        StringBuilder formatted = new StringBuilder();
        String section = "";
        boolean first = true;
        for (String line : yaml.saveToString().split("\n", -1)) {
            if (!line.isBlank() && !line.startsWith(" ") && !line.startsWith("#") && !line.startsWith("-")) {
                section = line.equals("updates:") ? "updates" : line.equals("plugins:") ? "plugins" : "";
                first = true;
            }
            boolean entry = section.equals("updates") && line.startsWith("- ")
                    || section.equals("plugins") && line.matches("  [^ #].*:.*");
            if (entry) {
                // Bukkit preserves blank lines before legacy keys as indented comments.
                int previousStart = formatted.lastIndexOf("\n", formatted.length() - 2) + 1;
                if (formatted.length() > 0 && formatted.substring(previousStart).isBlank())
                    formatted.replace(previousStart, formatted.length(), "\n");
                if (!first && (formatted.length() < 2 || formatted.charAt(formatted.length() - 2) != '\n')) formatted.append('\n');
                first = false;
            }
            formatted.append(line).append('\n');
        }
        // saveToString already terminates its last line.
        return formatted.substring(0, formatted.length() - 1);
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
