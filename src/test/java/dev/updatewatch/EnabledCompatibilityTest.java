package dev.updatewatch;

import com.google.gson.JsonArray;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnabledCompatibilityTest {
    private final ModrinthTest data = new ModrinthTest();
    private Remote.Source source(String installed, boolean enabled) {
        return new Remote.Source("Example", installed, "modrinth", "example", ".*\\.jar", "26.3", enabled);
    }
    private JsonArray releases() {
        var releases = new JsonArray();
        releases.add(data.version("2.0", "2026-10-01T00:00:00Z", "paper", "1.21.11", "release"));
        return releases;
    }
    @Test void enabledCurrentPluginIsCheckedWithoutGameFilterAndConsoleNoise() {
        var result = UpdateCheckService.checkSource(source("2.0", true), url -> {
            assertFalse(url.contains("game_versions=")); assertTrue(url.contains("loaders=")); return releases();
        });
        assertNull(result.error()); assertEquals(Versions.Status.CURRENT, result.status());
        assertTrue(result.release().compatibilityWarning().contains("does not list Minecraft 26.3"));
        assertFalse(ReportVisibility.result(true, result)); assertTrue(ReportVisibility.result(false, result));
        assertFalse(ReportVisibility.confirmedUpdate(result));
    }
    @Test void newerReleaseWithoutGameLabelIsAnUpdateWithWarning() {
        var result = UpdateCheckService.checkSource(source("1.0", true), url -> releases());
        assertEquals(Versions.Status.UPDATE, result.status()); assertTrue(ReportVisibility.confirmedUpdate(result));
        assertNotNull(result.release().download()); assertTrue(result.release().hasChecksum());
        assertNotNull(result.release().compatibilityWarning());
    }
    @Test void installedNewerThanPublishedIsCurrent() {
        var result = UpdateCheckService.checkSource(source("3.0", true), url -> releases());
        assertEquals(Versions.Status.CURRENT, result.status());
    }
    @Test void newestLoaderReleaseWinsEvenWhenOlderReleaseListsGame() throws Exception {
        var versions = releases();
        versions.add(data.version("1.0", "2026-09-01T00:00:00Z", "paper", "26.3", "release"));
        versions.add(data.version("9.0", "2026-10-05T00:00:00Z", "fabric", "26.3", "release"));
        var hidden = data.version("8.0", "2026-10-04T00:00:00Z", "paper", "26.3", "release");
        hidden.addProperty("status", "unlisted"); versions.add(hidden);
        assertEquals("2.0", Modrinth.select(versions, source("1.0", true)).version());
    }
    @Test void disabledPluginKeepsStrictGameFilter() {
        var result = UpdateCheckService.checkSource(source("2.0", false), url -> {
            assertTrue(url.contains("game_versions=")); return releases();
        });
        assertNull(result.status()); assertNotNull(result.error());
    }
    @Test void matchingGameNeedsNoCompatibilityWarning() throws Exception {
        var versions = new JsonArray(); versions.add(data.version("2.0", "2026-10-01T00:00:00Z", "bukkit", "26.3", "beta"));
        var release = Modrinth.select(versions, source("1.0", true));
        assertNull(release.compatibilityWarning()); assertEquals(ReleaseType.BETA, release.type());
    }
    @Test void enabledDoesNotTurnEmptyOrFailedResponsesIntoCurrent() {
        var empty = UpdateCheckService.checkSource(source("1.0", true), url -> new JsonArray());
        var failed = UpdateCheckService.checkSource(source("1.0", true), url -> { throw new Remote.HttpError(429); });
        assertNull(empty.status()); assertNotNull(empty.error()); assertNull(failed.status()); assertNotNull(failed.error());
    }
    @Test void runtimeEnabledStatePropagatesThroughExplicitAndLegacyConfiguration() throws Exception {
        for (boolean enabled : List.of(true, false)) {
            var installed = List.of(new Discovery.Installed("Example", "1.0", null, enabled));
            var jar = new Discovery.Jar(java.nio.file.Path.of("Example.jar"), "Example", "1.0");
            var config = new YamlConfiguration();
            config.set("updates", List.of(Map.of("jar", "Example.jar", "source", "https://modrinth.com/plugin/example")));
            assertEquals(enabled, ConfigSources.resolve(config, installed, List.of(jar), "26.3", false, h -> null).sources().getFirst().enabled());
            config.set("updates", null); config.set("plugins.Example.source", "modrinth"); config.set("plugins.Example.project", "example");
            assertEquals(enabled, ConfigSources.resolve(config, installed, List.of(jar), "26.3", false, h -> null).sources().getFirst().enabled());
        }
    }
}
