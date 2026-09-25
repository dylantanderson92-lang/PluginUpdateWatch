package dev.updatewatch;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import org.bukkit.configuration.file.YamlConfiguration;

final class Remote {
    record Source(String name, String installed, String type, String id, String asset, String minecraft) {}
    record Release(String version, String download, String page, String sha512) {
        Release(String version, String download, String page) { this(version, download, page, null); }
    }

    static InputStream open(String url) throws IOException {
        URI uri = URI.create(url);
        for (int redirects = 0; redirects < 6; redirects++) {
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IOException("Only HTTPS URLs are accepted");
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()
                        || (address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc))
                    throw new IOException("Non-public download address rejected");
            }
            HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(15000); c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "PluginUpdateWatch/1.1.0 (Paper plugin update checker)");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location"); c.disconnect();
                if (location == null) throw new IOException("Redirect missing location");
                uri = uri.resolve(location); continue;
            }
            if (code != 200) { c.disconnect(); throw new IOException("Source returned HTTP " + code); }
            return new FilterInputStream(c.getInputStream()) {
                @Override public void close() throws IOException { try { super.close(); } finally { c.disconnect(); } }
            };
        }
        throw new IOException("Too many redirects");
    }

    static JsonObject json(String url) throws IOException {
        return jsonValue(url).getAsJsonObject();
    }

    static JsonElement jsonValue(String url) throws IOException {
        try (InputStream in = open(url)) {
            byte[] bytes = in.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IOException("API response too large");
            return JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    static Release latest(Source s) throws IOException {
        if (s.type().equalsIgnoreCase("modrinth")) {
            return Modrinth.select(jsonValue(Modrinth.url(s)).getAsJsonArray(), s);
        }
        if (s.type().equalsIgnoreCase("spigot")) {
            if (!s.id().matches("[1-9][0-9]*")) throw new IOException("Invalid Spigot resource-id");
            String base = "https://api.spiget.org/v2/resources/" + s.id();
            JsonObject info = json(base), v = json(base + "/versions/latest");
            boolean blocked = (info.has("premium") && info.get("premium").getAsBoolean())
                    || (info.has("external") && info.get("external").getAsBoolean());
            return new Release(v.get("name").getAsString(), blocked ? null : base + "/versions/" + v.get("id").getAsLong() + "/download",
                    "https://www.spigotmc.org/resources/" + s.id() + "/");
        }
        if (s.type().equalsIgnoreCase("github")) {
            if (!s.id().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IOException("Invalid GitHub repository");
            JsonObject v = json("https://api.github.com/repos/" + s.id() + "/releases/latest");
            Pattern pattern = Pattern.compile(s.asset());
            List<String> matches = new ArrayList<>();
            for (JsonElement e : v.getAsJsonArray("assets")) {
                JsonObject a = e.getAsJsonObject(); String name = a.get("name").getAsString();
                if (name.endsWith(".jar") && pattern.matcher(name).matches()) matches.add(a.get("browser_download_url").getAsString());
            }
            return new Release(v.get("tag_name").getAsString(), matches.size() == 1 ? matches.getFirst() : null,
                    "https://github.com/" + s.id() + "/releases/latest");
        }
        throw new IOException("Source must be spigot, github or modrinth");
    }

    static Path download(Source source, Release release, Path folder) throws Exception {
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, ".download-", ".tmp");
        try {
            try (InputStream in = open(release.download()); OutputStream out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[8192]; long total = 0, deadline = System.nanoTime() + 120_000_000_000L;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    total += n;
                    if (total > 100L * 1024 * 1024 || System.nanoTime() > deadline) throw new IOException("Download exceeded size/time limit");
                    out.write(buffer, 0, n);
                }
            }
            verifyHash(temp, release.sha512());
            validate(temp, source.name());
            String filename = source.name().replaceAll("[^A-Za-z0-9_-]", "_") + "-" + release.version().replaceAll("[^A-Za-z0-9._-]", "_");
            filename = filename.substring(0, Math.min(filename.length(), 160));
            Path target = folder.resolve(filename + ".jar");
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } finally { Files.deleteIfExists(temp); }
    }

    static void verifyHash(Path path, String expected) throws Exception {
        if (expected == null) return;
        var digest = java.security.MessageDigest.getInstance("SHA-512");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        if (!HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(expected))
            throw new IOException("Downloaded file checksum does not match Modrinth");
    }

    static void validate(Path path, String expectedName) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry("paper-plugin.yml");
            if (entry == null) entry = jar.getJarEntry("plugin.yml");
            if (entry == null) throw new IOException("Downloaded file is not a Paper/Bukkit plugin JAR");
            try (InputStream in = jar.getInputStream(entry)) {
                byte[] bytes = in.readNBytes(65537);
                if (bytes.length > 65536) throw new IOException("Plugin descriptor too large");
                YamlConfiguration y = new YamlConfiguration();
                y.loadFromString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                if (!expectedName.equalsIgnoreCase(y.getString("name", ""))) throw new IOException("Downloaded JAR belongs to a different plugin");
                if (y.getString("main", "").isBlank()) throw new IOException("Plugin main class missing");
            }
        }
    }
}
