package dev.updatewatch;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

/** Official Bukkit endpoints have build metadata; pin the file to the checked build and its digest. */
final class GeyserRelease {
    private static final Pattern ENDPOINT = Pattern.compile("/v2/projects/(geyser|floodgate)/versions/latest/builds/latest/downloads/spigot");
    static String project(String url) {
        var uri = URI.create(url);
        if (!"download.geysermc.org".equalsIgnoreCase(uri.getHost()) || uri.getRawQuery() != null) return null;
        var match = ENDPOINT.matcher(uri.getRawPath());
        return match.matches() ? match.group(1) : null;
    }
    static Remote.Release latest(String endpoint, SourcePage.Fetch pages) throws IOException {
        String project = project(endpoint);
        if (project == null) throw Failure.problem(Failure.Kind.INVALID_CONFIG, "Unrecognized Geyser project endpoint");
        String metadata = endpoint.substring(0, endpoint.length() - "/downloads/spigot".length());
        SourcePage.Page page = pages.get(metadata);
        HttpTransport.webUri(page.url());
        try {
            if (page.jar()) throw new IllegalArgumentException("Expected build metadata");
            var build = JsonParser.parseString(page.html()).getAsJsonObject();
            String version = build.get("version").getAsString(), number = build.get("build").getAsString();
            var file = build.getAsJsonObject("downloads").getAsJsonObject("spigot");
            String name = file.get("name").getAsString(), hash = file.get("sha256").getAsString();
            if (!version.matches("[0-9]+(?:\\.[0-9]+)*(?:[-+][A-Za-z0-9.-]+)?") || !number.matches("[1-9][0-9]*")
                    || !Discovery.validFilename(name) || !hash.matches("[a-fA-F0-9]{64}"))
                throw new IllegalArgumentException("Invalid build metadata");
            String download = "https://download.geysermc.org/v2/projects/" + project + "/versions/" + version + "/builds/" + number + "/downloads/spigot";
            return new Remote.Release(version, download, "https://geysermc.org/download?project=" + project, null, hash, name, ReleaseType.fromLabel(version));
        } catch (RuntimeException e) {
            throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Geyser build metadata is missing a valid version, build, Spigot file or SHA-256 checksum", e);
        }
    }
}
