package dev.updatewatch;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportedSourcesTest {
    @Test void encodedSpigotTitlesResolveToResourceIds() {
        assertEquals("124687", SourceLink.parse("https://www.spigotmc.org/resources/timedflyx-%E2%80%93-controlled-temp-flight-perks-%E2%9C%88%EF%B8%8F-safe-configurable-async-%E2%9C%85-1-12-%E2%80%93-1-21.124687/").id());
        var creative = SourceLink.parse("https://www.spigotmc.org/resources/%E2%9A%A1-creativemanager-%E2%9A%A1-customizable-complete-control.75097/");
        assertEquals("spigot", creative.type()); assertEquals("75097", creative.id());
        assertEquals("75097", SourceLink.parse("https://www.spigotmc.org/resources/⚡-creativemanager.75097/").id());
    }
    @Test void encodedSeparatorsTraversalAndInvalidUtf8RemainRejected() {
        for (String path : List.of("resources/evil%2Ftitle.123/", "resources/%2e%2e.123/", "resources/evil%5cname.123/",
                "resources/evil%252fname.123/", "resources/%00.123/", "%72esources/name.123/", "resources/name.%31%32%33/",
                "resources/%FF.123/", "resources/%C0%AF.123/"))
            assertThrows(IllegalArgumentException.class, () -> SourceLink.parse("https://spigotmc.org/" + path), path);
        assertThrows(IllegalArgumentException.class, () -> SourceLink.parse("https://github.com/o/%E2%9A%A1"));
    }
    @Test void officialGeyserProjectPagesUseSpigotDownloadEndpoints() throws Exception {
        for (String project : List.of("geyser", "floodgate")) {
            var link = SourceLink.parse("https://geysermc.org/download?project=" + project);
            assertEquals("web", link.type());
            assertEquals("https://download.geysermc.org/v2/projects/" + project + "/versions/latest/builds/latest/downloads/spigot", link.id());
            var release = SourcePage.latest(link.source(project, "1.0", "26.3"),
                    url -> { assertEquals(link.id().replace("/downloads/spigot", ""), url); return new SourcePage.Page(url,
                            "{\"version\":\"2.2.5\",\"build\":141,\"downloads\":{\"spigot\":{\"name\":\"Example.jar\",\"sha256\":\"" + "a".repeat(64) + "\"}}}", false); },
                    url -> { fail("Must not query unrelated GitHub releases"); return null; });
            assertEquals(link.id().replace("versions/latest/builds/latest", "versions/2.2.5/builds/141"), release.download());
            assertEquals("2.2.5", release.version()); assertTrue(release.hasChecksum());
        }
    }
    @Test void otherGeyserProjectsAndLookalikesAreNotRewritten() {
        for (String url : List.of("https://geysermc.org/download?project=other", "https://geysermc.org/other?project=geyser",
                "https://geysermc.org.example.com/download?project=geyser", "https://geysermc.org/download?project=geyser&project=floodgate"))
            assertEquals(url, SourceLink.parse(url).id());
    }
    @Test void numberedDevelopmentBuildsCompareInBothDirections() {
        for (var pair : List.of(List.of("3.0.0-SNAPSHOT.88", "3.0.0-SNAPSHOT.90"), List.of("0.1.0-beta.1", "0.1.0-beta.2"),
                List.of("1.2-SNAPSHOT", "1.5"), List.of("1.0-alpha.9", "1.0-alpha.10"), List.of("1.0-rc.2", "1.0"))) {
            assertEquals(Versions.Status.UPDATE, Versions.compare(pair.get(0), pair.get(1)), pair.toString());
            assertEquals(Versions.Status.CURRENT, Versions.compare(pair.get(1), pair.get(0)), pair.toString());
        }
    }
    @Test void missingCountersCustomTagsAndHashesRemainUncertain() {
        for (var pair : List.of(List.of("5.12.1-SNAPSHOT", "5.12.1-SNAPSHOT+1069"), List.of("5.5.0-SNAPSHOT", "dev-build"),
                List.of("1.7.3-b131", "1.7.3"), List.of("1.0-beta.2+abc", "1.0-beta.2+def"), List.of("1.0-beta", "1.0-beta.2")))
            assertEquals(Versions.Status.DIFFERENT, Versions.compare(pair.get(0), pair.get(1)), pair.toString());
    }
    @Test void githubDevelopmentTagWarnsEvenWithoutPrereleaseFlag() throws Exception {
        var source = new Remote.Source("ProtocolLib", "5.5.0-SNAPSHOT", "github", "owner/repo", ".*\\.jar", "26.3");
        var release = Providers.latest(source, url -> GithubReleaseTest.list("{\"tag_name\":\"dev-build\",\"assets\":[]}"));
        assertEquals(ReleaseType.DEVELOPMENT, release.type()); assertTrue(release.type().warning);
    }
}
