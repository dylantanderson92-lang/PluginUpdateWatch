package dev.updatewatch;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class GeyserReleaseTest {
    @TempDir Path temp;
    String endpoint = "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot";
    String json(String version, String hash) {
        return "{\"version\":\"" + version + "\",\"build\":141,\"downloads\":{\"spigot\":{\"name\":\"floodgate-spigot.jar\",\"sha256\":\"" + hash + "\"}}}";
    }
    @Test void matchingInstalledDigestMakesBuildLabelDifferencesCurrent() throws Exception {
        String installed = "2.2.5-SNAPSHOT (b141-81b65cc)";
        Path jar = temp.resolve("Example.jar"); Files.write(jar, new ArtifactFreshnessTest().jar(installed, "installed"));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        var source = SourceLink.parse("https://geysermc.org/download?project=floodgate").source("Example", installed, "26.3");
        var result = UpdateCheckService.checkSource(source, url -> { fail(); return null; },
                url -> new SourcePage.Page(url, json("2.2.5", hash), false), List.of(Discovery.read(jar)));
        assertNull(result.error()); assertEquals(Versions.Status.CURRENT, result.status());
        assertTrue(result.release().download().contains("/versions/2.2.5/builds/141/"));
        assertEquals("floodgate-spigot.jar", result.release().filename());
        assertFalse(ReportVisibility.confirmedUpdate(result)); assertFalse(ReportVisibility.result(true, result));
    }
    @Test void installedNewerDevelopmentCoreDoesNotOfferADowngrade() {
        var source = SourceLink.parse(endpoint.replace("floodgate", "geyser")).source("Geyser-Spigot", "2.12.0-SNAPSHOT", "26.3");
        var result = UpdateCheckService.checkSource(source, url -> { fail(); return null; }, url -> new SourcePage.Page(url, json("2.11.3", "a".repeat(64)), false));
        assertNull(result.error()); assertEquals(Versions.Status.CURRENT, result.status());
    }
    @Test void invalidMetadataNeverFallsBackToUnverifiedDownload() {
        for (String body : List.of("{}", "<html>error</html>", json("../bad", "a".repeat(64)), json("2.2.5", "invalid"),
                json("2.2.5", "a".repeat(64)).replace("141", "1.5"), json("2.2.5", "a".repeat(64)).replace("floodgate-spigot.jar", "../bad.jar")))
            assertThrows(Failure.Problem.class, () -> GeyserRelease.latest(endpoint, url -> new SourcePage.Page(url, body, false)), body);
    }
    @Test void providerFailureStillFailsTheCheck() {
        var result = UpdateCheckService.checkSource(SourceLink.parse(endpoint).source("floodgate", "2.2.5", "26.3"), url -> { fail(); return null; },
                url -> { throw new Remote.HttpError(429); });
        assertNull(result.status()); assertNotNull(result.error());
    }
    @Test void otherHostsAndPlatformsAreNotTreatedAsTheOfficialBukkitEndpoint() {
        for (String url : List.of(endpoint.replace("download.geysermc.org", "download.geysermc.org.example.com"), endpoint + "?other=1",
                endpoint.replace("spigot", "velocity"), endpoint.replace("floodgate", "other")))
            assertNull(GeyserRelease.project(url));
    }
}
