package dev.updatewatch;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GithubReleaseTest {
    static JsonArray list(String json) {
        var release = JsonParser.parseString(json).getAsJsonObject();
        release.addProperty("draft", false); release.addProperty("prerelease", false);
        release.addProperty("published_at", "2026-10-01T00:00:00Z");
        var array = new JsonArray(); array.add(release); return array;
    }
    static JsonObject release(String version, String date, boolean prerelease, boolean draft) {
        var result = list("{\"tag_name\":\"" + version + "\",\"assets\":[]}").get(0).getAsJsonObject();
        result.addProperty("published_at", date); result.addProperty("prerelease", prerelease); result.addProperty("draft", draft);
        return result;
    }
    Remote.Source source = new Remote.Source("Example", "1", "github", "o/r", ".*", "26.3");
    @Test void latestPublishedReleaseIncludesPrereleasesAndExcludesDraftsRegardlessOfOrder() throws Exception {
        var versions = new JsonArray();
        versions.add(release("2.0", "2026-09-01T00:00:00Z", false, false));
        versions.add(release("9.0", "2026-10-05T00:00:00Z", false, true));
        versions.add(release("3.0-beta", "2026-10-01T00:00:00Z", true, false));
        var urls = new ArrayList<String>();
        var latest = Providers.latest(source, url -> { urls.add(url); return versions; });
        assertEquals(List.of("https://api.github.com/repos/o/r/releases?per_page=100"), urls);
        assertEquals("3.0-beta", latest.version()); assertEquals(ReleaseType.PRERELEASE, latest.type());
        assertEquals("https://github.com/o/r/releases/tag/3.0-beta", latest.page());
        assertTrue(latest.type().message().contains("[WARNING]"));
    }
    @Test void newerStableBeatsOlderPrerelease() throws Exception {
        var versions = new JsonArray();
        versions.add(release("9.0-beta", "2026-09-01T00:00:00Z", true, false));
        versions.add(release("2.0", "2026-10-01T00:00:00Z", false, false));
        var latest = Providers.latest(source, url -> versions);
        assertEquals("2.0", latest.version()); assertEquals(ReleaseType.STABLE, latest.type());
    }
    @Test void emptyOrUnpublishedFeedIsNotCurrent() {
        var result = UpdateCheckService.checkSource(source, url -> new JsonArray());
        assertNull(result.status()); assertTrue(result.error().contains("No published"));
    }
    @Test void unknownAndDevelopmentLabelsWarnWithoutClaimingStable() {
        assertEquals(ReleaseType.DEVELOPMENT, ReleaseType.fromLabel("EssentialsX-2.22.1-dev+27-e70bdb8.jar"));
        assertEquals(ReleaseType.DEVELOPMENT, ReleaseType.fromLabel("2.2.5-SNAPSHOT (b141)"));
        assertEquals(ReleaseType.RELEASE_CANDIDATE, ReleaseType.fromLabel("2.0-rc1"));
        assertEquals(ReleaseType.UNKNOWN, ReleaseType.fromLabel("2.0"));
        assertTrue(ReleaseType.UNKNOWN.message().contains("[WARNING]"));
    }
}
