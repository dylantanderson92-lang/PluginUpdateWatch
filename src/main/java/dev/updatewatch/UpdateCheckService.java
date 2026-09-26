package dev.updatewatch;

import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

/** Runs on the I/O executor. No access to the live plugin, scheduler or command senders. */
final class UpdateCheckService {
    record Result(Remote.Source source, Remote.Release release, Versions.Status status, String error) {}
    record Count(int success, int failure) {}
    record Report(ConfigSources.Resolution resolution, Map<String, Result> results, Map<String, Count> counts, long durationMillis) {}
    static Report check(String yaml, List<Discovery.Installed> installed, String minecraft, boolean scan, Settings settings) throws Exception {
        long started = System.nanoTime();
        var config = new YamlConfiguration(); config.loadFromString(yaml);
        var transport = new HttpTransport(settings);
        Providers.JsonFetch fetch = url -> Remote.jsonValue(url, settings, transport);
        Discovery.Lookup lookup = hash -> {
            try { return fetch.get("https://api.modrinth.com/v2/version_file/" + hash + "?algorithm=sha512").getAsJsonObject().get("project_id").getAsString(); }
            catch (Remote.HttpError e) { if (e.code == 404) return null; throw e; }
        };
        var resolution = ConfigSources.resolve(config, installed, Discovery.inventory(settings.pluginsFolder()), minecraft, scan, lookup);
        Map<String, Result> checked = new LinkedHashMap<>(); Map<String, Count> counts = new LinkedHashMap<>();
        for (var source : resolution.sources()) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Check cancelled");
            Result result;
            try {
                var release = Providers.latest(source, fetch);
                result = new Result(source, release, Versions.compare(source.installed(), release.version()), null);
            } catch (Exception e) { result = new Result(source, null, null, source.type() + ": " + message(e)); }
            checked.put(source.name().toLowerCase(Locale.ROOT), result);
            Count previous = counts.getOrDefault(source.type(), new Count(0, 0));
            counts.put(source.type(), new Count(previous.success() + (result.error() == null ? 1 : 0), previous.failure() + (result.error() != null ? 1 : 0)));
        }
        return new Report(resolution, Collections.unmodifiableMap(checked), Map.copyOf(counts), (System.nanoTime() - started) / 1_000_000);
    }
    static String message(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
