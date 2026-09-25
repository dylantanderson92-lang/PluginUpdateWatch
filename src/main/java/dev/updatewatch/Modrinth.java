package dev.updatewatch;

import com.google.gson.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

final class Modrinth {
    private static final Set<String> LOADERS = Set.of("paper", "spigot", "bukkit");

    static String url(Remote.Source source) throws IOException {
        if (!source.id().matches("[A-Za-z0-9_-]+")) throw new IOException("Modrinth project must be a slug or project ID, not a full URL");
        JsonArray versions = new JsonArray(); versions.add(source.minecraft());
        return "https://api.modrinth.com/v2/project/" + source.id() + "/version?loaders="
                + encode("[\"paper\",\"spigot\",\"bukkit\"]") + "&game_versions=" + encode(versions.toString()) + "&include_changelog=false";
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    static Remote.Release select(JsonArray versions, Remote.Source source) throws IOException {
        JsonObject latest = null;
        Instant newest = Instant.MIN;
        for (JsonElement element : versions) {
            JsonObject v = element.getAsJsonObject();
            if (!"release".equals(v.get("version_type").getAsString())) continue;
            if (v.has("status") && !"listed".equals(v.get("status").getAsString())) continue;
            boolean gameMatches = false, loaderMatches = false;
            for (JsonElement game : v.getAsJsonArray("game_versions")) if (source.minecraft().equals(game.getAsString())) gameMatches = true;
            for (JsonElement loader : v.getAsJsonArray("loaders")) if (LOADERS.contains(loader.getAsString())) loaderMatches = true;
            if (!gameMatches || !loaderMatches) continue;
            Instant published = Instant.parse(v.get("date_published").getAsString());
            if (published.isAfter(newest)) { newest = published; latest = v; }
        }
        if (latest == null) throw new IOException("No stable Paper/Spigot/Bukkit release listed for Minecraft " + source.minecraft());
        Pattern pattern = Pattern.compile(source.asset());
        List<JsonObject> files = new ArrayList<>(), primary = new ArrayList<>();
        for (JsonElement element : latest.getAsJsonArray("files")) {
            JsonObject file = element.getAsJsonObject();
            String name = file.get("filename").getAsString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".jar") || !pattern.matcher(name).matches()) continue;
            files.add(file);
            if (file.has("primary") && file.get("primary").getAsBoolean()) primary.add(file);
        }
        JsonObject selected = files.size() == 1 ? files.getFirst() : primary.size() == 1 ? primary.getFirst() : null;
        String hash = null;
        if (selected != null) {
            JsonObject hashes = selected.getAsJsonObject("hashes");
            if (hashes == null || !hashes.has("sha512") || !hashes.get("sha512").getAsString().matches("[a-fA-F0-9]{128}"))
                throw new IOException("Modrinth file is missing a valid SHA-512 checksum");
            hash = hashes.get("sha512").getAsString();
        }
        return new Remote.Release(latest.get("version_number").getAsString(), selected == null ? null : selected.get("url").getAsString(),
                "https://modrinth.com/plugin/" + source.id() + "/version/" + latest.get("id").getAsString(), hash);
    }
}
