package dev.updatewatch;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigFormattingTest {
    @TempDir Path temp;
    private final String yaml = """
            # Keep settings
            debug: false
            updates:
            - source: https://modrinth.com/plugin/first
              jar: First.jar
              note: |-
                multiline
                - not a row
            - jar: Second.jar
              source: ''
            - jar: Missing.jar
              source: ''
            plugins:
              LegacyOne:
                enabled: false
              LegacyTwo:
                source: github
                repository: owner/repo
            """;
    @Test void addsSpacesBetweenRowsAndLegacyPluginsWithoutChangingData() throws Exception {
        var parsed = ConfigManager.parse(yaml);
        var formatted = ConfigManager.serialize(parsed);
        assertTrue(formatted.contains("\n\n- jar: Second.jar"));
        assertTrue(formatted.contains("\n\n  LegacyTwo:"));
        assertEquals(parsed.getMapList("updates"), ConfigManager.parse(formatted).getMapList("updates"));
        var reparsed = ConfigManager.parse(formatted);
        for (String key : parsed.getKeys(true)) {
            if (!parsed.isConfigurationSection(key)) assertEquals(parsed.get(key), reparsed.get(key), key);
        }
        assertEquals(formatted, ConfigManager.serialize(ConfigManager.parse(formatted)));
    }
    @Test void scanSaveKeepsExactFormattedSnapshotAndRejectsConcurrentEdit() throws Exception {
        var path = temp.resolve("config.yml"); Files.writeString(path, yaml);
        String formatted = ConfigManager.serialize(ConfigManager.parse(yaml));
        ConfigManager.save(path, yaml, formatted); assertEquals(formatted, Files.readString(path));
        Files.writeString(path, "debug: true\n");
        assertThrows(java.io.IOException.class, () -> ConfigManager.save(path, formatted, yaml));
        assertEquals("debug: true\n", Files.readString(path));
    }
    @Test void cleanupKeepsSpacingAndExactOriginalBackup() throws Exception {
        Files.writeString(temp.resolve("First.jar"), "present"); Files.writeString(temp.resolve("Second.jar"), "present");
        var path = temp.resolve("config.yml"); Files.writeString(path, yaml);
        var plan = ConfigCleanup.plan(yaml, temp); var backup = ConfigCleanup.apply(path, temp, plan);
        assertEquals(yaml, Files.readString(backup));
        assertTrue(Files.readString(path).contains("\n\n- jar: Second.jar"));
        assertEquals(2, ConfigManager.parse(Files.readString(path)).getMapList("updates").size());
    }
    @Test void resolutionPreservesRowKeyOrderAcrossRepeatedScans() throws Exception {
        String text = ConfigManager.serialize(ConfigManager.parse(yaml));
        for (int i = 0; i < 3; i++) {
            var config = ConfigManager.parse(text);
            var resolution = ConfigSources.resolve(config, java.util.List.of(), java.util.List.of(), "26.3", true, hash -> null);
            assertEquals(java.util.List.of("source", "jar", "note"), new java.util.ArrayList<>(resolution.entries().getFirst().keySet()));
            assertEquals(java.util.List.of("jar", "source"), new java.util.ArrayList<>(resolution.entries().get(1).keySet()));
            config.set("updates", resolution.entries());
            assertEquals(text, ConfigManager.serialize(config));
            text = ConfigManager.serialize(config);
        }
    }
}
