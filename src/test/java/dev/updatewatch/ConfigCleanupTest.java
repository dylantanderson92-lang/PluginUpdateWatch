package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigCleanupTest {
    @TempDir Path temp;
    String yaml() {
        return "debug: true\nplugins:\n  Disabled:\n    enabled: false\nupdates:\n  - jar: old.jar\n    source: https://github.com/owner/repo\n    custom: keep-in-backup\n  - jar: Current.jar\n    source: https://modrinth.com/plugin/current\n";
    }
    @Test void previewKeepsPresentFilesAndDoesNotChangeConfig() throws Exception {
        Files.writeString(temp.resolve("Current.jar"), "unreadable archive still exists");
        var path = temp.resolve("config.yml"); Files.writeString(path, yaml());
        var plan = ConfigCleanup.plan(yaml(), temp);
        assertEquals(List.of("old.jar"), plan.missing()); assertEquals(yaml(), Files.readString(path));
        var parsed = ConfigManager.parse(plan.replacement());
        assertEquals(1, parsed.getMapList("updates").size()); assertTrue(parsed.getBoolean("debug"));
        assertFalse(parsed.getBoolean("plugins.Disabled.enabled"));
        assertEquals("https://modrinth.com/plugin/current", parsed.getMapList("updates").getFirst().get("source"));
    }
    @Test void confirmationBacksUpOriginalAndNeverDeletesFilesOrData() throws Exception {
        Files.writeString(temp.resolve("Current.jar"), "current bytes");
        var data = Files.createDirectory(temp.resolve("OldPluginData")); Files.writeString(data.resolve("data.txt"), "keep");
        var config = temp.resolve("config.yml"); Files.writeString(config, yaml());
        var backup = ConfigCleanup.apply(config, temp, ConfigCleanup.plan(yaml(), temp));
        assertEquals(yaml(), Files.readString(backup));
        assertEquals("current bytes", Files.readString(temp.resolve("Current.jar")));
        assertEquals("keep", Files.readString(data.resolve("data.txt")));
        assertEquals(1, ConfigManager.parse(Files.readString(config)).getMapList("updates").size());
    }
    @Test void configEditAfterPreviewRejectsCleanup() throws Exception {
        var config = temp.resolve("config.yml"); Files.writeString(config, yaml());
        var preview = ConfigCleanup.plan(yaml(), temp); Files.writeString(config, yaml() + "# user edit\n");
        assertThrows(Exception.class, () -> ConfigCleanup.apply(config, temp, preview));
        assertTrue(Files.readString(config).contains("# user edit")); assertFalse(Files.exists(temp.resolve("backups")));
    }
    @Test void reappearingJarAfterPreviewRejectsCleanup() throws Exception {
        var config = temp.resolve("config.yml"); Files.writeString(config, yaml());
        var preview = ConfigCleanup.plan(yaml(), temp); Files.writeString(temp.resolve("old.jar"), "returned");
        assertThrows(Exception.class, () -> ConfigCleanup.apply(config, temp, preview)); assertEquals(yaml(), Files.readString(config));
    }
    @Test void caseVariantAndDirectoryEntriesAreNotTreatedAsMissing() throws Exception {
        Files.writeString(temp.resolve("CURRENT.JAR"), "exists"); Files.createDirectory(temp.resolve("old.jar"));
        assertTrue(ConfigCleanup.plan(yaml(), temp).missing().isEmpty());
    }
    @Test void invalidRowsAndUnavailableFolderNeverCauseCleanup() throws Exception {
        for (String text : List.of("updates: bad", "updates: [oops]", "updates: [{jar: a.jar}]", "updates: ["))
            assertThrows(Exception.class, () -> ConfigCleanup.plan(text, temp));
        assertThrows(Exception.class, () -> ConfigCleanup.plan(yaml(), temp.resolve("absent")));
    }
    @Test void invalidPathsRemainForManualCorrection() throws Exception {
        var plan = ConfigCleanup.plan("updates:\n  - jar: ../outside.jar\n    source: ''\n", temp);
        assertTrue(plan.missing().isEmpty()); assertEquals(plan.original(), plan.replacement());
    }
}
