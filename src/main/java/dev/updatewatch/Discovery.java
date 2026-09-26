package dev.updatewatch;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.JarFile;

final class Discovery {
    record Installed(String name, String version, String website) {}
    record Jar(Path path, String name, String version) {}
    interface Lookup { String project(String hash) throws Exception; }

    static List<Jar> inventory(Path folder) throws IOException {
        List<Jar> jars = new ArrayList<>();
        try (var files = Files.list(folder)) {
            for (Path path : files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar") && Files.isRegularFile(p)).toList()) {
                try { jars.add(read(path)); } catch (Exception ignored) { /* Other/non-plugin files are not candidates. */ }
            }
        }
        return jars;
    }
    static Jar read(Path path) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry("paper-plugin.yml");
            if (entry == null) entry = jar.getJarEntry("plugin.yml");
            if (entry == null) throw new IOException("No plugin descriptor");
            try (InputStream in = jar.getInputStream(entry)) {
                byte[] bytes = in.readNBytes(65537);
                if (bytes.length > 65536) throw new IOException("Descriptor too large");
                YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(new String(bytes, StandardCharsets.UTF_8));
                String name = yaml.getString("name", ""), version = yaml.getString("version", "");
                if (name.isBlank() || version.isBlank() || yaml.getString("main", "").isBlank()) throw new IOException("Incomplete plugin descriptor");
                return new Jar(path, name, version);
            }
        }
    }
    static Jar match(List<Jar> jars, Installed plugin) {
        var matches = jars.stream().filter(j -> j.name().equals(plugin.name()) && j.version().equals(plugin.version())).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }
    static String hash(Path path) throws Exception {
        var digest = MessageDigest.getInstance("SHA-512");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                digest.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static String discover(Jar jar, Installed plugin, Lookup lookup) throws Exception {
        Exception failure = null;
        try {
            String project = lookup.project(hash(jar.path()));
            if (project != null && project.matches("[A-Za-z0-9_-]+")) return "https://modrinth.com/plugin/" + project;
        } catch (Exception e) { failure = e; }
        if (plugin.website() != null && !plugin.website().isBlank()) {
            try { SourceLink.parse(plugin.website()); return plugin.website(); } catch (IllegalArgumentException ignored) { }
        }
        if (failure != null) throw failure;
        return "";
    }
    static String modrinthProject(String hash) throws Exception {
        try { return Remote.json("https://api.modrinth.com/v2/version_file/" + hash + "?algorithm=sha512").get("project_id").getAsString(); }
        catch (Remote.HttpError e) { if (e.code == 404) return null; throw e; }
    }
    static boolean validFilename(String name) {
        return name != null && name.length() <= 240 && !name.isBlank() && !name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")
                && !name.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])\\..*")
                && !name.startsWith(".") && name.toLowerCase(Locale.ROOT).endsWith(".jar");
    }
}
