package dev.updatewatch;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class ModrinthTest {
    @TempDir Path temp;
    Remote.Source source = new Remote.Source("Example", "1.0", "modrinth", "example-plugin", ".*\\.jar", "1.21.11");
    JsonObject version(String number, String date, String loader, String game, String channel) {
        JsonObject v = new JsonObject();
        v.addProperty("id", "version" + number); v.addProperty("version_number", number);
        v.addProperty("date_published", date); v.addProperty("version_type", channel); v.addProperty("status", "listed");
        JsonArray loaders = new JsonArray(); loaders.add(loader); v.add("loaders", loaders);
        JsonArray games = new JsonArray(); games.add(game); v.add("game_versions", games);
        JsonArray files = new JsonArray(); files.add(file("plugin.jar", true)); v.add("files", files);
        return v;
    }
    JsonObject file(String name, boolean primary) {
        JsonObject f = new JsonObject(); f.addProperty("filename", name); f.addProperty("primary", primary);
        f.addProperty("url", "https://cdn.modrinth.com/" + name);
        JsonObject hashes = new JsonObject(); hashes.addProperty("sha512", "a".repeat(128)); f.add("hashes", hashes); return f;
    }
    JsonObject stable() { return version("2.0", "2026-09-20T00:00:00Z", "paper", "1.21.11", "release"); }
    @Test void filtersAndSortsWithoutTrustingApiOrder() throws Exception {
        JsonArray versions = new JsonArray(); versions.add(stable());
        versions.add(version("9.0", "2026-09-25T00:00:00Z", "fabric", "1.21.11", "release"));
        versions.add(version("8.0", "2026-09-25T00:00:00Z", "paper", "26.3", "release"));
        versions.add(version("7.0", "2026-09-25T00:00:00Z", "paper", "1.21.11", "beta"));
        versions.add(version("1.5", "2026-09-19T00:00:00Z", "bukkit", "1.21.11", "release"));
        assertEquals("2.0", Modrinth.select(versions, source).version());
    }
    @Test void noCompatibleVersionIsNotReportedCurrent() {
        assertThrows(IOException.class, () -> Modrinth.select(new JsonArray(), source));
    }
    @Test void choosesPrimaryJarAndLeavesAmbiguousFilesManual() throws Exception {
        JsonObject v = stable(); v.getAsJsonArray("files").add(file("alternate.jar", false));
        JsonArray versions = new JsonArray(); versions.add(v);
        assertEquals("https://cdn.modrinth.com/plugin.jar", Modrinth.select(versions, source).download());
        v.getAsJsonArray("files").get(0).getAsJsonObject().addProperty("primary", false);
        assertNull(Modrinth.select(versions, source).download());
    }
    @Test void regexCanSelectOneArtifact() throws Exception {
        JsonObject v = stable(); v.getAsJsonArray("files").add(file("alternate.jar", false));
        JsonArray versions = new JsonArray(); versions.add(v);
        var selectedSource = new Remote.Source("Example", "1.0", "modrinth", "example-plugin", "alternate\\.jar", "1.21.11");
        assertEquals("https://cdn.modrinth.com/alternate.jar", Modrinth.select(versions, selectedSource).download());
    }
    @Test void requestEncodesGameAndLoaderFilters() throws Exception {
        String url = java.net.URLDecoder.decode(Modrinth.url(source), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(url.contains("game_versions=[\"1.21.11\"]"));
        assertTrue(url.contains("loaders=[\"paper\",\"spigot\",\"bukkit\"]"));
        assertThrows(IOException.class, () -> Modrinth.url(new Remote.Source("E", "1", "modrinth", "../bad", ".*", "1.21.11")));
    }
    @Test void verifiesChecksumAndRejectsMismatch() throws Exception {
        Path p = temp.resolve("download.jar"); Files.writeString(p, "test data");
        String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(p)));
        Remote.verifyHash(p, hash);
        assertThrows(IOException.class, () -> Remote.verifyHash(p, "0".repeat(128)));
    }
    @Test void rejectsMissingChecksum() {
        JsonObject v = stable(); v.getAsJsonArray("files").get(0).getAsJsonObject().remove("hashes");
        JsonArray versions = new JsonArray(); versions.add(v);
        assertThrows(IOException.class, () -> Modrinth.select(versions, source));
    }
}
