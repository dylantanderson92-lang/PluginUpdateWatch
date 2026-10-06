package dev.updatewatch;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class SourcePageTest {
    @TempDir Path temp;
    Remote.Source source(String url) { return new Remote.Source("Example", "1", "web", url, ".*", "26.3"); }
    Providers.JsonFetch noJson = url -> { fail("Unexpected API request: " + url); return null; };
    @Test void acceptsDirectEndpointsWikiAndEncodedWebPaths() {
        for (String url : List.of("https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot",
                "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot",
                "https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.22.1-dev+27-e70bdb8.jar",
                "https://example.org/wiki/My%20Plugin#Downloads", "https://github.com/owner/project/wiki/Downloads"))
            assertEquals("web", SourceLink.parse(url).type());
        assertEquals("github", SourceLink.parse("https://github.com/owner/project").type());
    }
    @Test void unsafeWebSourcesAndMalformedProviderSourcesAreRejected() {
        for (String url : List.of("http://example.org/file.jar", "https://user:secret@example.org/file.jar", "https://localhost/a",
                "https://127.0.0.1/file.jar", "https://[::1]/a", "https://example.local/a", "https://example.org:8443/a",
                "https://github.com/owner/../a", "https://modrinth.com/invalid"))
            assertThrows(IllegalArgumentException.class, () -> SourceLink.parse(url), url);
    }
    @Test void directJarPreservesBuildCharactersWithoutGuessingVersion() throws Exception {
        String url = "https://ci.example.org/Example-2.0+abc.jar";
        var r = SourcePage.latest(source(url), u -> { fail(); return null; }, noJson);
        assertEquals("Example-2.0+abc.jar", r.filename()); assertEquals(SourcePage.UNKNOWN_VERSION, r.version());
        assertEquals(url, r.download()); assertFalse(r.hasChecksum());
    }
    @Test void extensionlessBinaryUsesPlainPluginFilenameAndUnknownStatus() {
        var r = UpdateCheckService.checkSource(source("https://example.org/latest/download"), noJson,
                u -> new SourcePage.Page("https://cdn.example.org/build", "", true));
        assertNull(r.error()); assertEquals(Versions.Status.UNKNOWN, r.status());
        assertEquals("Example.jar", r.release().filename()); assertEquals("https://cdn.example.org/build", r.release().download());
    }
    @Test void relativeLinksUseFinalResponseUrlAndHtmlEntities() throws Exception {
        var r = SourcePage.latest(source("https://example.org/wiki"), u -> new SourcePage.Page("https://example.org/docs/guide/",
                "<a href='../files/Example%20Plugin.jar?build=2&amp;platform=paper#download'>Download</a>", false), noJson);
        assertEquals("https://example.org/docs/files/Example%20Plugin.jar?build=2&platform=paper", r.download());
        assertEquals("Example Plugin.jar", r.filename());
    }
    @Test void providerLinkRetainsVersionAndChecksum() throws Exception {
        String sha = "a".repeat(64);
        var r = SourcePage.latest(source("https://example.org/wiki"),
                u -> new SourcePage.Page(u, "<a href='fallback.jar'>Download</a><a href='https://github.com/o/r'>Releases</a>", false),
                u -> GithubReleaseTest.list("{\"tag_name\":\"2.0\",\"assets\":[{\"name\":\"Example.jar\",\"browser_download_url\":\"https://github.com/o/r/releases/download/v2/Example.jar\",\"digest\":\"sha256:" + sha + "\"}]}"));
        assertEquals("2.0", r.version()); assertEquals(sha, r.sha256());
    }
    @Test void failedProviderFallsBackToDirectJar() throws Exception {
        var r = SourcePage.latest(source("https://example.org/wiki"), u -> new SourcePage.Page(u,
                "<a href='https://github.com/o/r'>Releases</a><a href='Example.jar'>Download</a>", false),
                u -> { throw new Remote.HttpError(404); });
        assertEquals("https://example.org/Example.jar", r.download()); assertFalse(r.hasChecksum());
    }
    @Test void followsDownloadLandingPageButNeverScriptsOrPrivateLinks() throws Exception {
        var calls = new ArrayList<String>();
        var r = SourcePage.latest(source("https://example.org/wiki"), u -> {
            calls.add(u);
            return new SourcePage.Page(u, calls.size() == 1
                    ? "<script>fetch('https://evil.test/evil.jar')</script><a href='http://example.org/a.jar'>bad</a><a href='https://127.0.0.1/a.jar'>bad</a><a href='/get' download>Download</a>"
                    : "<a href='/Example-sources.jar'>Sources</a><a href='/Example.jar'>Plugin</a>", false);
        }, noJson);
        assertEquals(List.of("https://example.org/wiki", "https://example.org/get"), calls);
        assertEquals("https://example.org/Example.jar", r.download());
    }
    @Test void boundedCrawlAndLoopsProduceActionableFailure() {
        var calls = new AtomicInteger();
        var e = assertThrows(Failure.Problem.class, () -> SourcePage.latest(source("https://example.org/wiki"),
                u -> new SourcePage.Page(u, "<a href='/download/" + calls.incrementAndGet() + "'>Download</a><a href='/wiki'>Download</a>", false), noJson));
        assertEquals(3, calls.get()); assertTrue(e.getMessage().contains("direct JAR"));
    }
    @Test void retriesAndFinalUrlRemainAvailableForWebBodies() throws Exception {
        var count = new AtomicInteger(); var waits = new ArrayList<Long>();
        var transport = new HttpTransport(Settings.defaults(), u -> {
            try { return new HttpTransportTest.Response(count.getAndIncrement() == 0 ? 503 : 200, Map.of()); }
            catch (Exception e) { throw new IOException(e); }
        }, waits::add);
        var page = SourcePage.read("https://example.org/wiki", Settings.defaults(), transport);
        assertEquals("https://example.org/wiki", page.url()); assertFalse(page.jar());
        assertEquals(List.of(250L), waits);
    }
    @Test void webRedirectsRejectUnsafeDestinationsBeforeConnecting() throws Exception {
        for (String location : List.of("http://example.org/file.jar", "https://127.0.0.1/file.jar",
                "https://localhost/file.jar", "https://user@example.org/file.jar", "https://example.org:444/a", "https://example.org/start")) {
            var response = new HttpTransportTest.Response(302, Map.of("Location", location));
            var count = new AtomicInteger();
            var transport = new HttpTransport(Settings.defaults(), u -> { count.incrementAndGet(); return response; }, ms -> fail());
            assertThrows(IOException.class, () -> transport.openWeb("https://example.org/start", 30), location);
            assertEquals(1, count.get()); assertTrue(response.closed);
        }
    }
    @Test void webChecksumExceptionIsScopedAndCanBeDisabled() {
        var config = new YamlConfiguration();
        assertTrue(Settings.allowUnverifiedWeb(config));
        assertFalse(Settings.checksumRequired(source("https://example.org/plugin.jar"), true, true));
        assertTrue(Settings.checksumRequired(source("https://example.org/plugin.jar"), true, false));
        for (String provider : List.of("github", "spigot", "modrinth"))
            assertTrue(Settings.checksumRequired(new Remote.Source("Example", "1", provider, "id", ".*", "26.3"), true, true));
        config.set("downloads.allow-unverified-web", "yes");
        assertThrows(IllegalArgumentException.class, () -> Settings.allowUnverifiedWeb(config));
    }
    @Test void unverifiedWebDownloadsStillRejectWrongPluginsHtmlAndMismatchedHashes() throws Exception {
        var s = source("https://example.org/plugin.jar");
        var r = new Remote.Release(SourcePage.UNKNOWN_VERSION, s.id(), s.id(), null, null, "Example.jar");
        byte[] good = new DownloadFilenameTest().jar("Example", "");
        var saved = DownloadManager.download(s, r, temp, Settings.defaults(), false, (u, t) -> new ByteArrayInputStream(good));
        assertArrayEquals(good, Files.readAllBytes(saved));
        for (byte[] bad : List.of("<html>login</html>".getBytes(), new DownloadFilenameTest().jar("Other", ""))) {
            assertThrows(Exception.class, () -> DownloadManager.download(s, r, temp, Settings.defaults(), false, (u, t) -> new ByteArrayInputStream(bad)));
            assertArrayEquals(good, Files.readAllBytes(saved));
        }
        var hashed = new Remote.Release(r.version(), r.download(), r.page(), null, "0".repeat(64), r.filename());
        assertThrows(Exception.class, () -> DownloadManager.download(s, hashed, temp, Settings.defaults(), false, (u, t) -> new ByteArrayInputStream(good)));
        assertArrayEquals(good, Files.readAllBytes(saved));
    }
    @Test void pageProbeStopsAtZipSignatureAndRejectsOversizedHtml() throws Exception {
        var binary = new HttpTransportTest.Response(200, Map.of()) {
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[]{'P', 'K', 3, 4}) {
                @Override public synchronized int read(byte[] b, int off, int len) { assertTrue(len == 0 || pos < 4, "Must not fetch full binary during check"); return super.read(b, off, len); }
            }; }
        };
        assertTrue(SourcePage.read("https://example.org/latest", Settings.defaults(), new HttpTransport(Settings.defaults(), u -> binary, ms -> fail())).jar());
        var large = new HttpTransportTest.Response(200, Map.of()) {
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(new byte[2 * 1024 * 1024 + 1]); }
        };
        assertThrows(Failure.Problem.class, () -> SourcePage.read("https://example.org/wiki", Settings.defaults(),
                new HttpTransport(Settings.defaults(), u -> large, ms -> fail())));
    }
    @Test void manualWebConfigResolvesWithoutAutoTrustingMetadataHomepages() throws Exception {
        var config = new YamlConfiguration();
        String url = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot";
        config.set("updates", List.of(Map.of("jar", "Example.jar", "source", url)));
        var jar = new Discovery.Jar(temp.resolve("Example.jar"), "Example", "1");
        var installed = new Discovery.Installed("Example", "1", "https://example.org/wiki");
        var resolution = ConfigSources.resolve(config, List.of(installed), List.of(jar), "26.3", true, hash -> { fail(); return null; });
        assertEquals("web", resolution.sources().getFirst().type()); assertTrue(resolution.notes().isEmpty());
        Files.write(jar.path(), new DownloadFilenameTest().jar("Example", ""));
        assertEquals("", Discovery.discover(jar, installed, hash -> null));
    }
    @Test void redirectedPageUsesFinalUriAndProvidersRemainHostRestricted() throws Exception {
        var redirect = new HttpTransportTest.Response(302, Map.of("Location", "https://cdn.example.org/docs/"));
        var ok = new HttpTransportTest.Response(200, Map.of());
        var transport = new HttpTransport(Settings.defaults(), u -> u.getHost().equals("example.org") ? redirect : ok, ms -> fail());
        assertEquals("https://cdn.example.org/docs/", SourcePage.read("https://example.org/wiki", Settings.defaults(), transport).url());
        assertTrue(redirect.closed); assertTrue(ok.closed);
        assertThrows(IOException.class, () -> transport.open("https://example.org/wiki", 30));
    }
}
