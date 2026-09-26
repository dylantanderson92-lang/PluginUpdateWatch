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
    record Inventory(List<Jar> jars, Map<String, String> notes) {
        Inventory {
            jars = List.copyOf(jars);
            notes = Collections.unmodifiableMap(new LinkedHashMap<>(notes));
        }
        Inventory excludingPlugin(String name) {
            return new Inventory(jars.stream().filter(j -> !normalizedName(j.name()).equals(normalizedName(name))).toList(), notes);
        }
    }
    record Match(Jar jar, String note) {}
    interface Lookup { String project(String hash) throws Exception; }

    static List<Jar> inventory(Path folder) throws IOException {
        return inspect(folder).jars();
    }
    static Inventory inspect(Path folder) throws IOException {
        List<Jar> jars = new ArrayList<>();
        Map<String, String> notes = new LinkedHashMap<>();
        try (var files = Files.list(folder)) {
            for (Path path : files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).toList()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Discovery cancelled");
                String key = "JAR: " + path.getFileName();
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    notes.put(key, "Skipped: not a regular file (symbolic links are not scanned)"); continue;
                }
                try { jars.add(read(path)); }
                catch (DescriptorException e) { notes.put(key, e.getMessage()); }
                catch (java.util.zip.ZipException e) { notes.put(key, "Unreadable or invalid JAR archive; inspect or replace this file"); }
                catch (Exception e) { notes.put(key, "Cannot read plugin metadata (" + e.getClass().getSimpleName() + "); inspect the JAR and file permissions"); }
            }
        }
        return new Inventory(jars, notes);
    }
    private static final class DescriptorException extends IOException {
        private static final long serialVersionUID = 1L;
        DescriptorException(String message) { super(message); }
    }
    static Jar read(Path path) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            var entry = jar.getJarEntry("paper-plugin.yml");
            if (entry == null) entry = jar.getJarEntry("plugin.yml");
            if (entry == null) throw new DescriptorException("Not a plugin JAR: no plugin.yml or paper-plugin.yml descriptor");
            try (InputStream in = jar.getInputStream(entry)) {
                byte[] bytes = in.readNBytes(65537);
                if (bytes.length > 65536) throw new DescriptorException("Plugin descriptor exceeds 64 KiB; discovery skipped");
                YamlConfiguration yaml = new YamlConfiguration(); yaml.loadFromString(new String(bytes, StandardCharsets.UTF_8));
                String name = yaml.getString("name", ""), version = yaml.getString("version", "");
                List<String> missing = new ArrayList<>();
                if (!(yaml.get("name") instanceof String) || name.isBlank()) missing.add("name");
                if (!(yaml.get("version") instanceof String || yaml.get("version") instanceof Number) || version.isBlank()) missing.add("version");
                if (!(yaml.get("main") instanceof String) || yaml.getString("main", "").isBlank()) missing.add("main");
                if (!missing.isEmpty()) throw new DescriptorException("Missing or invalid plugin metadata: " + String.join(", ", missing) + "; discovery skipped");
                return new Jar(path, name, version);
            }
        }
    }
    static Jar match(List<Jar> jars, Installed plugin) {
        return matchReport(jars, plugin).jar();
    }
    static Match matchReport(List<Jar> jars, Installed plugin) {
        if (normalizedName(plugin.name()).isEmpty() || normalizedVersion(plugin.version()).isEmpty())
            return new Match(null, "Installed plugin has missing name/version metadata; cannot match a JAR safely");
        var named = jars.stream().filter(j -> normalizedName(j.name()).equals(normalizedName(plugin.name())))
                .sorted(Comparator.comparing(j -> j.path().getFileName().toString())).toList();
        var matches = named.stream().filter(j -> metadataMatches(j, plugin)).toList();
        if (matches.size() == 1) {
            var jar = matches.getFirst();
            String note = jar.name().equals(plugin.name()) && jar.version().equals(plugin.version()) ? null
                    : "Matched " + jar.path().getFileName() + " after normalizing name case/whitespace and version whitespace; original metadata is preserved";
            return new Match(jar, note);
        }
        if (matches.size() > 1) return new Match(null, "Ambiguous JAR metadata: duplicate name/version in " + filenames(matches)
                + "; remove duplicate JARs before scanning (no source was selected)");
        if (!named.isEmpty()) return new Match(null, "JAR metadata version mismatch: installed " + plugin.version()
                + "; found " + String.join(", ", named.stream().map(j -> j.path().getFileName() + " (" + j.version() + ")").toList())
                + "; check for old JARs or restart after an upgrade");
        return new Match(null, "No JAR metadata matches the installed plugin name/version; check for moved JARs or invalid descriptors");
    }
    static boolean metadataMatches(Jar jar, Installed plugin) {
        return !normalizedName(jar.name()).isEmpty() && !normalizedVersion(jar.version()).isEmpty()
                && normalizedName(jar.name()).equals(normalizedName(plugin.name()))
                && normalizedVersion(jar.version()).equals(normalizedVersion(plugin.version()));
    }
    static String normalizedName(String name) { return name == null ? "" : name.trim().toLowerCase(Locale.ROOT); }
    private static String normalizedVersion(String version) { return version == null ? "" : version.trim(); }
    private static String filenames(List<Jar> jars) {
        return String.join(", ", jars.stream().map(j -> j.path().getFileName().toString()).toList());
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
