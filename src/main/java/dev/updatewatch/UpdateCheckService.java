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
            try {
                String project = fetch.get("https://api.modrinth.com/v2/version_file/" + hash + "?algorithm=sha512").getAsJsonObject().get("project_id").getAsString();
                if (!project.matches("[A-Za-z0-9_-]+")) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Modrinth discovery returned an invalid project ID");
                return project;
            }
            catch (Remote.HttpError e) { if (e.code == 404) return null; throw e; }
            catch (RuntimeException e) { throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Modrinth discovery response is missing valid project metadata", e); }
        };
        var resolution = ConfigSources.resolve(config, installed, Discovery.inspect(settings.pluginsFolder()).excludingPlugin("PluginUpdateWatch"), minecraft, scan, lookup);
        Map<String, Result> checked = new LinkedHashMap<>(); Map<String, Count> counts = new LinkedHashMap<>();
        for (var source : resolution.sources()) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Check cancelled");
            Result result = checkSource(source, fetch);
            checked.put(source.name().toLowerCase(Locale.ROOT), result);
            Count previous = counts.getOrDefault(source.type(), new Count(0, 0));
            counts.put(source.type(), new Count(previous.success() + (result.error() == null ? 1 : 0), previous.failure() + (result.error() != null ? 1 : 0)));
        }
        return new Report(resolution, Collections.unmodifiableMap(checked), Map.copyOf(counts), (System.nanoTime() - started) / 1_000_000);
    }
    static Result checkSource(Remote.Source source, Providers.JsonFetch fetch) {
        try {
            var release = Providers.latest(source, fetch);
            return new Result(source, release, Versions.compare(source.installed(), release.version()), null);
        } catch (Exception e) { return new Result(source, null, null, Failure.classify(e).describe(source.type())); }
    }
    static String message(Exception e) { return Failure.classify(e).describe(null); }
}
