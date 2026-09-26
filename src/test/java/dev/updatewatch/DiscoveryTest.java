package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryTest {
    @TempDir Path temp;
    Discovery.Installed plugin = new Discovery.Installed("Example", "1.0", null);
    Discovery.Jar jar() throws Exception {
        Path p = temp.resolve("Example-1.0.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(p))) {
            out.putNextEntry(new JarEntry("plugin.yml")); out.write("name: Example\nversion: '1.0'\nmain: example.Main\n".getBytes()); out.closeEntry();
        }
        return Discovery.read(p);
    }
    ConfigSources.Resolution resolve(YamlConfiguration y, Discovery.Jar j, boolean scan, Discovery.Lookup lookup) {
        return ConfigSources.resolve(y, List.of(plugin), List.of(j), "26.3", scan, lookup);
    }
    @Test void addsAndPersistsHashMatchUsingTwoFields() throws Exception {
        var result = resolve(new YamlConfiguration(), jar(), true, hash -> { assertEquals(128, hash.length()); return "AbCd1234"; });
        assertEquals(Map.of("jar", "Example-1.0.jar", "source", "https://modrinth.com/plugin/AbCd1234"), result.entries().getFirst());
        assertEquals("AbCd1234", result.sources().getFirst().id());
        var saved = new YamlConfiguration(); saved.set("updates", result.entries());
        var reloaded = new YamlConfiguration(); reloaded.loadFromString(saved.saveToString());
        var again = resolve(reloaded, jar(), true, h -> { fail("Existing sources must not be rediscovered"); return null; });
        assertEquals(result.entries(), again.entries());
    }
    @Test void unresolvedEntryOnlyNeedsLink() throws Exception {
        var result = resolve(new YamlConfiguration(), jar(), true, hash -> null);
        assertEquals("", result.entries().getFirst().get("source")); assertTrue(result.notes().containsKey("Example"));
        var y = new YamlConfiguration(); y.set("updates", List.of(Map.of("jar", "Example-1.0.jar", "source", "https://www.spigotmc.org/resources/example.123/")));
        assertEquals("123", resolve(y, jar(), false, h -> null).sources().getFirst().id());
    }
    @Test void preservesLegacyAndDisabledSettings() throws Exception {
        var y = new YamlConfiguration(); y.set("plugins.Example.source", "modrinth"); y.set("plugins.Example.project", "old-project");
        var r = resolve(y, jar(), true, h -> { fail("Legacy source must stay intact"); return null; });
        assertEquals("old-project", r.sources().getFirst().id()); assertTrue(r.entries().isEmpty());
        y.set("plugins.Example.enabled", false);
        assertTrue(resolve(y, jar(), true, h -> null).sources().isEmpty());
    }
    @Test void doesNotGuessBetweenDuplicateOriginalJars() throws Exception {
        var first = jar(); Files.copy(first.path(), temp.resolve("duplicate.jar"));
        assertNull(Discovery.match(Discovery.inventory(temp), plugin));
        assertNull(Discovery.match(List.of(first), new Discovery.Installed("Example", "2.0", null)));
    }
    @Test void recognizesMetadataLinkWhenHashUnknown() throws Exception {
        assertEquals("https://github.com/owner/repo", Discovery.discover(jar(), new Discovery.Installed("Example", "1.0", "https://github.com/owner/repo"), h -> null));
    }
    @Test void reportsNetworkFailureInsteadOfClaimingNoMatch() throws Exception {
        var r = resolve(new YamlConfiguration(), jar(), true, h -> { throw new Remote.HttpError(429); });
        assertTrue(r.notes().get("Example").contains("HTTP 429")); assertTrue(r.sources().isEmpty());
    }
    @Test void duplicateManualEntriesAreNotSilentlySelected() throws Exception {
        var y = new YamlConfiguration(); var row = Map.of("jar", "Example-1.0.jar", "source", "https://modrinth.com/plugin/example");
        y.set("updates", List.of(row,row)); var r = resolve(y, jar(), true, h -> null);
        assertTrue(r.sources().isEmpty()); assertTrue(r.notes().get("Example").contains("Duplicate"));
    }
    @Test void rejectsInvalidFilenamesAndConfigTypes() throws Exception {
        assertFalse(Discovery.validFilename("../Example.jar")); assertFalse(Discovery.validFilename("C:\\Example.jar"));
        var y = new YamlConfiguration(); y.set("updates", "not a list"); var j = jar();
        assertThrows(IllegalArgumentException.class, () -> resolve(y, j, true, h -> null));
    }
    @Test void checksDoNotRunAutomaticDiscovery() throws Exception {
        var r = resolve(new YamlConfiguration(), jar(), false, h -> { fail(); return null; });
        assertTrue(r.entries().isEmpty()); assertTrue(r.notes().containsKey("Example"));
    }
    @Test void acceptsProjectLinksAndRejectsLookalikeHosts() {
        assertEquals("example", SourceLink.parse("https://modrinth.com/plugin/example/versions").id());
        assertEquals("123", SourceLink.parse("https://www.spigotmc.org/resources/example.123/updates").id());
        assertEquals("owner/repo", SourceLink.parse("https://github.com/owner/repo/releases/latest").id());
        assertTrue(java.util.regex.Pattern.matches(SourceLink.parse("https://github.com/owner/repo/releases/download/v1/plugin.jar").asset(), "plugin.jar"));
        assertThrows(IllegalArgumentException.class, () -> SourceLink.parse("https://modrinth.com.evil.test/plugin/example"));
        assertThrows(IllegalArgumentException.class, () -> SourceLink.parse("https://github.com@evil.test/owner/repo"));
        assertThrows(IllegalArgumentException.class, () -> SourceLink.parse("http://modrinth.com/plugin/example"));
    }
    @Test void duplicateJarMetadataIsExplainedAndNoSourceIsGuessed() throws Exception {
        var original = jar(); Files.copy(original.path(), temp.resolve("duplicate.jar"));
        var result = ConfigSources.resolve(new YamlConfiguration(), List.of(plugin), Discovery.inspect(temp), "26.3", true, h -> { fail("Ambiguous JARs must not be hashed"); return null; });
        assertTrue(result.sources().isEmpty()); assertTrue(result.entries().isEmpty());
        String note = result.notes().get("Example");
        assertTrue(note.contains("Ambiguous")); assertTrue(note.contains("Example-1.0.jar")); assertTrue(note.contains("duplicate.jar"));
    }
    @Test void normalizationPreservesMetadataAndRejectsCollisions() {
        var first = new Discovery.Jar(temp.resolve("first.jar"), " example ", " 1.0 ");
        assertEquals(first, Discovery.match(List.of(first), plugin)); assertEquals(" example ", first.name());
        assertNotNull(Discovery.matchReport(List.of(first), plugin).note());
        var second = new Discovery.Jar(temp.resolve("second.jar"), "EXAMPLE", "1.0");
        assertNull(Discovery.match(List.of(first, second), plugin));
        assertNull(Discovery.match(List.of(new Discovery.Jar(temp.resolve("beta.jar"), "Example", "1.0-beta")), plugin));
        assertNull(Discovery.match(List.of(new Discovery.Jar(temp.resolve("zero.jar"), "Example", "01.0")), plugin));
    }
    @Test void mismatchAndOrphanMetadataProduceActionableNotes() {
        var old = new Discovery.Jar(temp.resolve("old.jar"), "Example", "0.9");
        var orphan = new Discovery.Jar(temp.resolve("orphan.jar"), "AnotherPlugin", "1.0");
        var result = ConfigSources.resolve(new YamlConfiguration(), List.of(plugin), List.of(old, orphan), "26.3", true, h -> null);
        assertTrue(result.notes().get("Example").contains("version mismatch"));
        assertTrue(result.notes().get("JAR: old.jar").contains("No installed plugin"));
        assertTrue(result.notes().get("JAR: orphan.jar").contains("startup logs"));
        assertTrue(result.sources().isEmpty());
    }
    @Test void invalidArchivesAndDescriptorsAppearInInventoryNotes() throws Exception {
        Files.writeString(temp.resolve("broken.jar"), "not a zip");
        try (var out = new JarOutputStream(Files.newOutputStream(temp.resolve("library.jar")))) { out.putNextEntry(new JarEntry("hello.txt")); out.closeEntry(); }
        try (var out = new JarOutputStream(Files.newOutputStream(temp.resolve("missing.jar")))) { out.putNextEntry(new JarEntry("plugin.yml")); out.write("name: Missing\n".getBytes()); out.closeEntry(); }
        var inventory = Discovery.inspect(temp); assertTrue(inventory.jars().isEmpty()); assertEquals(3, inventory.notes().size());
        var result = ConfigSources.resolve(new YamlConfiguration(), List.of(), inventory, "26.3", true, h -> null);
        assertEquals(inventory.notes(), result.notes());
    }
    @Test void explicitLegacySourceRemainsUsableDespiteAmbiguousJars() throws Exception {
        var original = jar(); Files.copy(original.path(), temp.resolve("duplicate.jar"));
        var y = new YamlConfiguration(); y.set("plugins.Example.source", "modrinth"); y.set("plugins.Example.project", "example");
        var result = ConfigSources.resolve(y, List.of(plugin), Discovery.inspect(temp), "26.3", true, h -> { fail(); return null; });
        assertEquals(1, result.sources().size()); assertTrue(result.notes().get("Example").contains("Ambiguous"));
    }
}
