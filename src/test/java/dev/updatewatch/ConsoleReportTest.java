package dev.updatewatch;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleReportTest {
    UpdateCheckService.Result result(Versions.Status status, ReleaseType type, String checksum, String download) {
        var source = new Remote.Source("Example", "1.0", "web", "https://example.org/file.jar", ".*", "26.3");
        var release = new Remote.Release("2.0", download, "https://example.org/releases", checksum, null, "Example.jar", type);
        return new UpdateCheckService.Result(source, release, status, null);
    }
    @Test void missingSourceHasShortSpecificHelpWithoutRepeatingYamlInstructions() {
        String text = ConsoleReport.note("DylyDoubleBeds", "Source not identified; paste a source link for DylyDoubleBeds.jar in config.yml");
        assertTrue(text.contains("source link missing")); assertTrue(text.endsWith("#missing-source"));
        assertFalse(text.contains("paste a source link")); assertFalse(text.contains("\n"));
    }
    @Test void errorsKeepTheirClassificationAndCorrectWikiTopic() {
        for (var kind : List.of(Failure.Kind.NETWORK, Failure.Kind.RATE_LIMIT, Failure.Kind.INVALID_CONFIG, Failure.Kind.INVALID_ARTIFACT, Failure.Kind.ALREADY_INSTALLED)) {
            var failure = new Failure(kind, "Controlled failure detail", 10);
            String text = ConsoleReport.error("Example", failure.describe("github"));
            assertTrue(text.contains("Controlled failure detail")); assertTrue(text.endsWith(Troubleshooting.failure(kind)));
            assertFalse(text.contains("before retrying")); assertEquals(1, text.split("Help:", -1).length - 1);
        }
    }
    @Test void confirmedUpdatesRetainReleaseTypeDownloadActionAndManualReleaseLink() {
        String beta = ConsoleReport.result(result(Versions.Status.UPDATE, ReleaseType.BETA, "a".repeat(128), "https://example.org/file.jar"), true);
        assertTrue(beta.contains("[UPDATE] Example: 1.0 -> 2.0")); assertTrue(beta.contains("[beta]"));
        assertTrue(beta.contains("/pu download Example")); assertTrue(beta.endsWith("#prereleases")); assertFalse(beta.contains("this may be"));
        String manual = ConsoleReport.result(result(Versions.Status.UPDATE, ReleaseType.UNKNOWN, null, "https://example.org/file.jar"), true);
        assertTrue(manual.contains("manual download (no checksum)")); assertTrue(manual.contains("https://example.org/releases"));
        assertTrue(manual.endsWith("#checksums")); assertFalse(manual.contains("type: unknown"));
    }
    @Test void downloadWarningsAreConsolidatedWithoutLosingInspectionOrChecksumCautions() {
        String start = ConsoleReport.downloadStart(result(Versions.Status.UNKNOWN, ReleaseType.DEVELOPMENT, null, "https://example.org/file.jar"));
        assertTrue(start.contains("inspection only")); assertTrue(start.contains("checksum unverified"));
        assertEquals(1, start.split("\\[WARNING\\]", -1).length - 1); assertTrue(start.endsWith("#checksums"));
        String saved = ConsoleReport.downloadSaved(result(Versions.Status.UNKNOWN, ReleaseType.STABLE, "a".repeat(128), "https://example.org/file.jar"),
                Path.of("downloads/Example.jar"), "1.0", ReleaseType.STABLE);
        assertTrue(saved.contains("checksum verified")); assertTrue(saved.contains("inspection only")); assertTrue(saved.startsWith("[WARNING]"));
        assertTrue(saved.endsWith("#unconfirmed-versions"));
    }
    @Test void onlyVerifiedIdenticalDownloadsMarkCachedResultsCurrent() {
        var original = result(Versions.Status.UNKNOWN, ReleaseType.DEVELOPMENT, null, "https://example.org/file.jar");
        var current = ReportVisibility.afterDownload(original, new Failure(Failure.Kind.ALREADY_INSTALLED, "same file", 0));
        assertEquals(Versions.Status.CURRENT, current.status()); assertFalse(ReportVisibility.result(true, current));
        assertFalse(ReportVisibility.confirmedUpdate(current)); assertSame(original.source(), current.source());
        assertSame(original, ReportVisibility.afterDownload(original, new Failure(Failure.Kind.NETWORK, "timeout", 0)));
        assertSame(original, ReportVisibility.afterDownload(original, new Failure(Failure.Kind.NO_NEWER_ARTIFACT, "older", 0)));
    }
    @Test void summaryUsesOneDetailsCommandAndDoesNotDescribeUnconfirmedAsCurrent() {
        String text = ConsoleReport.summary(2, 2, 1, 4);
        assertTrue(text.contains("2 update(s), 2 need source/configuration, 1 failed check(s), 4 unconfirmed"));
        assertTrue(text.contains("/pu list all")); assertTrue(text.endsWith("#unconfirmed-versions")); assertFalse(text.contains("up to date"));
        assertFalse(ConsoleReport.summary(0, 0, 0, 0).contains("0 intentionally disabled"));
    }
}
