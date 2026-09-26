package dev.updatewatch;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

final class ConfigSources {
    record Resolution(List<Remote.Source> sources, Map<String, String> notes, List<Map<String, Object>> entries) {}
    static Resolution resolve(YamlConfiguration config, List<Discovery.Installed> installed, List<Discovery.Jar> jars,
                              String minecraft, boolean scan, Discovery.Lookup lookup) {
        return resolve(config, installed, new Discovery.Inventory(jars, Map.of()), minecraft, scan, lookup);
    }
    static Resolution resolve(YamlConfiguration config, List<Discovery.Installed> installed, Discovery.Inventory inventory,
                              String minecraft, boolean scan, Discovery.Lookup lookup) {
        var jars = inventory.jars();
        if (config.contains("updates") && !config.isList("updates")) throw new IllegalArgumentException("updates must be a YAML list of jar/source entries");
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Object row : config.getList("updates", List.of())) {
            if (!(row instanceof Map<?,?> map) || !(map.get("jar") instanceof String) || !(map.get("source") instanceof String))
                throw new IllegalArgumentException("Each updates row must contain string jar and source fields (source may be blank)");
        }
        for (Map<?, ?> row : config.getMapList("updates")) {
            Map<String, Object> copy = new LinkedHashMap<>(); row.forEach((k,v) -> copy.put(k.toString(), v)); entries.add(copy);
        }
        List<Remote.Source> sources = new ArrayList<>(); Map<String, String> notes = new LinkedHashMap<>(inventory.notes());
        for (var jar : jars) {
            if (installed.stream().noneMatch(p -> Discovery.metadataMatches(jar, p)))
                notes.put("JAR: " + jar.path().getFileName(), "No installed plugin matches metadata " + jar.name() + " " + jar.version()
                        + "; check for an old, disabled or renamed plugin JAR and review server startup logs");
        }
        Set<String> invalid = new HashSet<>(), seen = new HashSet<>();
        for (var entry : entries) {
            String filename = (String) entry.get("jar"), key = filename.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) { invalid.add(key); notes.put("Config: " + filename, "Duplicate jar entries; keep only one source per JAR"); }
            if (!Discovery.validFilename(filename)) { invalid.add(key); notes.put("Config: " + filename, "Invalid JAR filename"); }
            String link = (String) entry.get("source");
            if (!link.isBlank()) try { SourceLink.parse(link); }
            catch (IllegalArgumentException e) { invalid.add(key); notes.put("Config: " + filename, Failure.classify(e).describe(null)); }
        }
        for (var p : installed) {
            var match = Discovery.matchReport(jars, p);
            var jar = match.jar();
            if (match.note() != null) notes.put(p.name(), match.note());
            var legacy = config.getConfigurationSection("plugins." + p.name());
            var matching = jar == null ? List.<Map<String,Object>>of() : entries.stream()
                    .filter(e -> jar.path().getFileName().toString().equalsIgnoreCase(Objects.toString(e.get("jar"), ""))).toList();
            if (matching.size() > 1) { notes.put(p.name(), "Duplicate config entries for " + jar.path().getFileName()); continue; }
            if (jar != null && invalid.contains(jar.path().getFileName().toString().toLowerCase(Locale.ROOT))) { notes.put(p.name(), "Invalid config entry; see config warnings"); continue; }
            Map<String,Object> entry = matching.isEmpty() ? null : matching.getFirst();
            // Existing legacy entries remain authoritative unless the user supplies a new explicit link.
            if (legacy != null && (entry == null || Objects.toString(entry.get("source"), "").isBlank())) {
                if (!legacy.getBoolean("enabled", true)) { notes.put(p.name(), "Disabled in existing configuration"); continue; }
                String type = legacy.getString("source", "spigot");
                try {
                    var source = new Remote.Source(p.name(), p.version(), type, legacy.getString(type.equalsIgnoreCase("github") ? "repository"
                            : type.equalsIgnoreCase("modrinth") ? "project" : "resource-id", ""), legacy.getString("asset-regex", ".*\\.jar"), minecraft);
                    Providers.validate(source); sources.add(source);
                } catch (IllegalArgumentException e) { notes.put(p.name(), "Invalid legacy config. " + Failure.classify(e).describe(type)); }
                continue;
            }
            if (jar == null) continue;
            if (entry == null && scan) {
                entry = new LinkedHashMap<>(); entry.put("jar", jar.path().getFileName().toString()); entry.put("source", ""); entries.add(entry);
            }
            if (entry == null) { notes.put(p.name(), "Not configured; run /pu scan"); continue; }
            String link = Objects.toString(entry.get("source"), "").trim();
            if (link.isBlank() && scan) {
                try { link = Discovery.discover(jar, p, lookup); entry.put("source", link); }
                catch (Exception e) { notes.put(p.name(), "Discovery failed: " + Failure.classify(e).describe("modrinth") + " Paste a source link for " + jar.path().getFileName() + " if discovery remains unavailable."); continue; }
            }
            if (link.isBlank()) { notes.put(p.name(), "Source not identified; paste a source link for " + jar.path().getFileName() + " in config.yml"); continue; }
            try { sources.add(SourceLink.parse(link).source(p.name(), p.version(), minecraft)); }
            catch (Exception e) { notes.put(p.name(), "Invalid source link for " + jar.path().getFileName() + ": " + Failure.classify(e).describe(null)); }
        }
        for (var entry : entries) {
            String filename = Objects.toString(entry.get("jar"), "");
            if (invalid.contains(filename.toLowerCase(Locale.ROOT))) continue;
            if (!Discovery.validFilename(filename)) notes.put("Config: " + filename, "jar must be a filename ending in .jar, without a folder path");
            else if (jars.stream().noneMatch(j -> j.path().getFileName().toString().equalsIgnoreCase(filename)
                    && installed.stream().anyMatch(p -> Discovery.metadataMatches(j, p))))
                notes.put("Config: " + filename, "JAR does not match an installed plugin; update the filename if it changed");
        }
        return new Resolution(List.copyOf(sources), Collections.unmodifiableMap(new LinkedHashMap<>(notes)), entries.stream().map(Map::copyOf).toList());
    }
}
