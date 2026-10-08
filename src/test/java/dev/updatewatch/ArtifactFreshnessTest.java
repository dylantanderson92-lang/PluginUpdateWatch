package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class ArtifactFreshnessTest {
    @TempDir Path temp;
    byte[] jar(String version, String marker) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new JarOutputStream(bytes)) {
            out.putNextEntry(new JarEntry("plugin.yml")); out.write(("name: Example\nversion: '" + version + "'\nmain: example.Main\n# " + marker).getBytes()); out.closeEntry();
            out.putNextEntry(new JarEntry("example/Main.class"));
            try (var in = getClass().getResourceAsStream("/dev/updatewatch/ArtifactFreshnessTest.class")) { in.transferTo(out); }
            out.closeEntry();
        }
        return bytes.toByteArray();
    }
    Path download(String installedVersion, String advertisedVersion, byte[] bytes, Path installed) throws Exception {
        var source = new Remote.Source("Example", installedVersion, "web", "https://example.org/latest", ".*", "26.3");
        var release = new Remote.Release(advertisedVersion, source.id(), source.id(), null, null, "Example.jar");
        return DownloadManager.download(source, release, temp.resolve("downloads"), Settings.defaults(), false,
                (url, seconds) -> new ByteArrayInputStream(bytes), installed);
    }
    @Test void rejectsIdenticalInstalledJarEvenWhenSourceClaimsNewerVersion() throws Exception {
        byte[] bytes = jar("1.0-SNAPSHOT", "original");
        Path installed = temp.resolve("installed.jar"); Files.write(installed, bytes);
        var error = assertThrows(Failure.Problem.class, () -> download("1.0-SNAPSHOT", "2.0", bytes, installed));
        assertTrue(error.getMessage().contains("byte-for-byte"));
        assertArrayEquals(bytes, Files.readAllBytes(installed));
        try (var files = Files.list(temp.resolve("downloads"))) { assertEquals(0, files.count()); }
    }
    @Test void rejectsSameOrOlderJarBehindAdvertisedNewerReleaseWithoutOverwritingPreviousDownload() throws Exception {
        byte[] valid = jar("2.0", "valid"); Path saved = download("1.0", "2.0", valid, null);
        for (String stale : List.of("1.0", "0.9", "paper-1.0")) {
            assertThrows(Failure.Problem.class, () -> download("1.0", "2.0", jar(stale, "stale"), null));
            assertArrayEquals(valid, Files.readAllBytes(saved));
        }
    }
    @Test void unknownWebSourceCannotDownloadANumericDowngrade() throws Exception {
        assertThrows(Failure.Problem.class, () -> download("2.0", SourcePage.UNKNOWN_VERSION, jar("1.0", "old"), null));
    }
    @Test void changingSnapshotWithSameDescriptorVersionIsAllowedForExplicitInspection() throws Exception {
        Path installed = temp.resolve("installed.jar"); Files.write(installed, jar("1.0-SNAPSHOT", "old"));
        byte[] next = jar("1.0-SNAPSHOT", "different build");
        Path saved = download("1.0-SNAPSHOT", SourcePage.UNKNOWN_VERSION, next, installed);
        assertArrayEquals(next, Files.readAllBytes(saved));
    }
    @Test void genuineNewerPlanBuildIsAcceptedButOldBuildIsRejected() throws Exception {
        assertNotNull(download("5.8 build 3605", "5.8+build.3638", jar("5.8 build 3638", "new"), null));
        assertThrows(Failure.Problem.class, () -> download("5.8 build 3638", "5.8+build.3640", jar("5.8 build 3638", "same version"), null));
    }
    @Test void numberedPrereleaseUpdatesRejectStaleDownloads() throws Exception {
        for (var versions : List.of(List.of("3.0.0-SNAPSHOT.88", "3.0.0-SNAPSHOT.90"), List.of("0.1.0-beta.1", "0.1.0-beta.2"))) {
            byte[] next = jar(versions.get(1), "new");
            Path saved = download(versions.get(0), versions.get(1), next, null);
            assertThrows(Failure.Problem.class, () -> download(versions.get(0), versions.get(1), jar(versions.get(0), "stale"), null));
            assertArrayEquals(next, Files.readAllBytes(saved));
        }
    }
}
