package dev.updatewatch;

import com.google.gson.JsonArray;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class InstalledArtifactTest {
    @TempDir Path temp;
    final Remote.Source source = new Remote.Source("Example", "0.1.0-beta.1", "modrinth", "example", ".*\\.jar", "26.3", true);
    Discovery.Jar installed() throws Exception {
        Path path = temp.resolve("Example.jar");
        Files.write(path, new ArtifactFreshnessTest().jar(source.installed(), "installed"));
        return Discovery.read(path);
    }
    String hash(Path path, String algorithm) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(Files.readAllBytes(path)));
    }
    JsonArray versions(String sha512) {
        var version = new ModrinthTest().version("0.1.0-beta.2", "2026-10-08T00:00:00Z", "paper", "1.21.11", "beta");
        version.getAsJsonArray("files").get(0).getAsJsonObject().getAsJsonObject("hashes").addProperty("sha512", sha512);
        var versions = new JsonArray(); versions.add(version); return versions;
    }
    UpdateCheckService.Result check(List<Discovery.Jar> jars, String hash) {
        return UpdateCheckService.checkSource(source, url -> versions(hash), url -> { fail("No artifact downloads during checks"); return null; }, jars);
    }
    @Test void identicalReleaseChecksumOverridesMislabelledNewerVersion() throws Exception {
        var jar = installed(); var result = check(List.of(jar), hash(jar.path(), "SHA-512").toUpperCase(Locale.ROOT));
        assertNull(result.error()); assertEquals(Versions.Status.CURRENT, result.status());
        assertEquals("0.1.0-beta.2", result.release().version()); assertEquals("0.1.0-beta.1", result.source().installed());
        assertFalse(ReportVisibility.result(true, result)); assertFalse(ReportVisibility.confirmedUpdate(result));
    }
    @Test void differentChecksumKeepsNewerReleaseAsUpdate() throws Exception {
        var result = check(List.of(installed()), "0".repeat(128));
        assertNull(result.error()); assertEquals(Versions.Status.UPDATE, result.status());
    }
    @Test void missingOrAmbiguousInstalledJarCannotProveCurrent() throws Exception {
        var jar = installed(); var hash = hash(jar.path(), "SHA-512");
        assertEquals(Versions.Status.UPDATE, check(List.of(), hash).status());
        assertEquals(Versions.Status.UPDATE, check(List.of(jar, new Discovery.Jar(temp.resolve("Duplicate.jar"), jar.name(), jar.version())), hash).status());
    }
    @Test void replacedJarMetadataCannotProveCurrentFromOldInventory() throws Exception {
        var jar = installed(); Files.write(jar.path(), new ArtifactFreshnessTest().jar("0.1.0-beta.3", "replacement"));
        assertEquals(Versions.Status.UPDATE, check(List.of(jar), hash(jar.path(), "SHA-512")).status());
    }
    @Test void supportsGithubSha256AndRequiresEverySuppliedDigestToMatch() throws Exception {
        var jar = installed(); String sha256 = hash(jar.path(), "SHA-256");
        var release = new Remote.Release("2.0", "https://example.org/file.jar", "https://example.org", null, sha256);
        assertTrue(InstalledArtifact.matches(source, release, List.of(jar)));
        var conflicting = new Remote.Release("2.0", release.download(), release.page(), "0".repeat(128), sha256);
        assertFalse(InstalledArtifact.matches(source, conflicting, List.of(jar)));
        assertFalse(InstalledArtifact.matches(source, new Remote.Release("2.0", release.download(), release.page()), List.of(jar)));
    }
    @Test void missingFileOrInvalidHashDoesNotBecomeCurrent() throws Exception {
        var jar = installed(); var invalid = check(List.of(jar), "invalid");
        assertNotNull(invalid.error()); assertNull(invalid.status());
        Files.delete(jar.path()); var missing = check(List.of(jar), "0".repeat(128));
        assertNotNull(missing.error()); assertNull(missing.status());
    }
}
