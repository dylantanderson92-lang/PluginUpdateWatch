package dev.updatewatch;

import java.nio.file.*;
import java.util.*;

/** Explicit cleanup of missing-file config rows; never removes plugin files or data. */
final class ConfigCleanup {
    record Plan(String original, String replacement, List<String> missing) {}
    static Plan plan(String contents, Path pluginsFolder) throws Exception {
        var config = ConfigManager.parse(contents);
        if (config.contains("updates") && !config.isList("updates")) throw new IllegalArgumentException("updates must be a YAML list");
        Set<String> present = new HashSet<>();
        // Include unreadable files, directories and symlinks: absence is the only cleanup criterion.
        try (var files = Files.list(pluginsFolder)) {
            files.forEach(p -> present.add(p.getFileName().toString().toLowerCase(Locale.ROOT)));
        }
        List<Object> keep = new ArrayList<>(); List<String> missing = new ArrayList<>();
        for (Object row : config.getList("updates", List.of())) {
            if (!(row instanceof Map<?, ?> entry) || !(entry.get("jar") instanceof String filename) || !(entry.get("source") instanceof String))
                throw new IllegalArgumentException("Each updates row must contain string jar and source fields");
            if (Discovery.validFilename(filename) && !present.contains(filename.toLowerCase(Locale.ROOT))) missing.add(filename);
            else keep.add(row);
        }
        if (missing.isEmpty()) return new Plan(contents, contents, List.of());
        config.set("updates", keep);
        return new Plan(contents, config.saveToString(), List.copyOf(missing));
    }
    static Path apply(Path configFile, Path pluginsFolder, Plan preview) throws Exception {
        Plan current = plan(Files.readString(configFile), pluginsFolder);
        if (!current.equals(preview)) throw new java.io.IOException("Config or missing-file list changed; run /pu cleanup again before confirming");
        if (current.missing().isEmpty()) throw new java.io.IOException("No missing-file entries to clean");
        Path backups = configFile.getParent().resolve("backups");
        Files.createDirectories(backups);
        Path backup = Files.createTempFile(backups, "config-before-cleanup-", ".yml");
        Files.writeString(backup, current.original());
        ConfigManager.save(configFile, current.original(), current.replacement());
        return backup;
    }
}
