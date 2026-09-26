package dev.updatewatch;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.*;

class FailureTest {
    private Remote.Source source(String provider, String id) { return new Remote.Source("Example", "1.0", provider, id, ".*\\.jar", "26.3"); }
    @Test void networkAndProviderFailuresHaveDifferentActions() {
        String network = Failure.classify(new SocketTimeoutException("https://secret?token=private")).describe("github");
        assertTrue(network.contains("[ERROR] NETWORK_ERROR")); assertTrue(network.contains("retry /pu check")); assertFalse(network.contains("private"));
        String rate = Failure.classify(new Remote.HttpError(429, "api.github.com", 120)).describe("github");
        assertTrue(rate.contains("PROVIDER_ERROR")); assertTrue(rate.contains("120 seconds")); assertTrue(rate.contains("GitHub"));
        String denied = Failure.classify(new Remote.HttpError(403)).describe("modrinth");
        assertTrue(denied.contains("denied access")); assertTrue(denied.contains("source link"));
    }
    @Test void malformedApiDataIsNotMisclassifiedAsUserConfig() {
        var result = UpdateCheckService.checkSource(source("github", "owner/repo"), url -> JsonParser.parseString("{}"));
        assertNull(result.status()); assertNull(result.release());
        assertTrue(result.error().contains("PROVIDER_ERROR")); assertFalse(result.error().contains("CONFIG_ERROR"));
    }
    @Test void invalidSourceIsConfigErrorBeforeAnyNetworkCall() {
        var result = UpdateCheckService.checkSource(source("github", "../bad"), url -> { fail("Bad configuration must not make requests"); return null; });
        assertTrue(result.error().contains("CONFIG_ERROR")); assertTrue(result.error().contains("/pu reload")); assertNull(result.status());
    }
    @Test void requestFailureCannotBeReportedAsCurrent() {
        var result = UpdateCheckService.checkSource(source("github", "owner/repo"), url -> { throw new IOException("unexpected response body and signed URL"); });
        assertTrue(result.error().contains("NETWORK_ERROR")); assertNull(result.status()); assertFalse(result.error().contains("signed URL"));
    }
    @Test void noCompatibleReleaseHasUnknownUpdateStatus() {
        var result = UpdateCheckService.checkSource(source("modrinth", "example"), url -> JsonParser.parseString("[]"));
        assertNull(result.status()); assertTrue(result.error().contains("[WARNING]")); assertTrue(result.error().contains("update status is unknown"));
    }
    @Test void successfulCurrentAndNewerReleaseAreSeparateFromErrors() {
        var current = UpdateCheckService.checkSource(source("github", "owner/repo"), url -> JsonParser.parseString("{\"tag_name\":\"1.0\",\"assets\":[]}"));
        var newer = UpdateCheckService.checkSource(source("github", "owner/repo"), url -> JsonParser.parseString("{\"tag_name\":\"2.0\",\"assets\":[]}"));
        assertNull(current.error()); assertEquals(Versions.Status.CURRENT, current.status());
        assertNull(newer.error()); assertEquals(Versions.Status.UPDATE, newer.status());
    }
    @Test void nestedExplicitArtifactClassificationPreservesContextAndRedactsUrls() {
        var failure = Failure.classify(new IOException(Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Invalid file at https://cdn.modrinth.com/file?token=secret\nrejected")));
        assertEquals(Failure.Kind.INVALID_ARTIFACT, failure.kind());
        String text = failure.describe("modrinth"); assertTrue(text.contains("Download was rejected")); assertFalse(text.contains("secret")); assertFalse(text.contains("\n"));
    }
}
