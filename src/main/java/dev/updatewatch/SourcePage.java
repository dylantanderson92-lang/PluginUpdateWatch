package dev.updatewatch;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.Jsoup;

/** Best-effort, bounded HTML link discovery. Never executes scripts or infers release versions from filenames. */
final class SourcePage {
    static final String UNKNOWN_VERSION = "unknown (web source)";
    private static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;
    record Page(String url, String html, boolean jar) {}
    interface Fetch { Page get(String url) throws IOException; }
    private record Link(String url, int priority, SourceLink provider) {}

    static Page read(String url, Settings settings, HttpTransport transport) throws IOException {
        try (InputStream in = transport.openWeb(url, settings.metadataSeconds())) {
            String finalUrl = in instanceof HttpTransport.Body body ? body.uri.toString() : url;
            byte[] prefix = in.readNBytes(4);
            // Probe only the ZIP signature during checks; full CRC, descriptor and class validation happens at download.
            if (Arrays.equals(prefix, new byte[]{'P', 'K', 3, 4})) return new Page(finalUrl, "", true);
            byte[] rest = in.readNBytes(MAX_PAGE_BYTES - prefix.length + 1);
            if (rest.length + prefix.length > MAX_PAGE_BYTES)
                throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Source page exceeded the 2 MiB limit");
            byte[] bytes = Arrays.copyOf(prefix, prefix.length + rest.length);
            System.arraycopy(rest, 0, bytes, prefix.length, rest.length);
            return new Page(finalUrl, new String(bytes, StandardCharsets.UTF_8), false);
        } catch (Remote.HttpError | Failure.Problem e) { throw e; }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Could not read the HTTPS source page or download endpoint", e); }
    }

    static Remote.Release latest(Remote.Source source, Fetch pages, Providers.JsonFetch json) throws IOException {
        String initial;
        try { initial = HttpTransport.webUri(source.id()).toString(); }
        catch (IOException e) { throw Failure.problem(Failure.Kind.INVALID_CONFIG, "Invalid HTTPS web source", e); }
        var queue = new ArrayDeque<String>(); queue.add(initial);
        var visited = new HashSet<String>();
        IOException lastFailure = null;
        int requests = 0, providers = 0;
        while (!queue.isEmpty() && requests < 3) {
            String url = queue.removeFirst();
            if (!visited.add(url)) continue;
            if (filename(url) != null) return unversioned(source, url, initial);
            Page page;
            requests++;
            try { page = pages.get(url); }
            catch (IOException e) { lastFailure = e; continue; }
            HttpTransport.webUri(page.url());
            visited.add(page.url());
            if (page.jar()) return unversioned(source, page.url(), initial);
            var document = Jsoup.parse(page.html(), page.url());
            var links = new LinkedHashMap<String, Link>();
            int examined = 0;
            for (var anchor : document.select("a[href]")) {
                if (++examined > 512) break;
                String href = anchor.attr("abs:href").split("#", 2)[0];
                try { HttpTransport.webUri(href); } catch (IOException ignored) { continue; }
                SourceLink provider = null;
                try { var parsed = SourceLink.parse(href); if (!parsed.type().equals("web")) provider = parsed; }
                catch (IllegalArgumentException ignored) { }
                String file = filename(href);
                String hint = (anchor.text() + " " + URI.create(href).getPath()).toLowerCase(Locale.ROOT);
                if (file != null && file.toLowerCase(Locale.ROOT).matches(".*-(?:sources|javadoc)\\.jar")) continue;
                int priority = provider != null ? 0 : file != null ? 1 : 2;
                if (provider == null && file == null && !anchor.hasAttr("download") && !hint.contains("download")) continue;
                links.putIfAbsent(href, new Link(href, priority, provider));
            }
            // Prefer provider metadata/checksums when available, then direct JARs, then download landing pages.
            for (Link link : links.values().stream().sorted(Comparator.comparingInt(Link::priority)).toList()) {
                if (link.provider() != null) {
                    if (providers++ >= 3) continue;
                    try {
                        var release = Providers.latest(link.provider().source(source.name(), source.installed(), source.minecraft()), json);
                        if (release.download() != null) return release;
                    } catch (IOException e) { lastFailure = e; }
                } else if (filename(link.url()) != null) return unversioned(source, link.url(), initial);
                else if (queue.size() < 32 && !visited.contains(link.url())) queue.addLast(link.url());
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "No usable download link found within three source pages; paste a direct JAR/download endpoint or a provider project link (scripts and login pages are not supported)");
    }
    private static Remote.Release unversioned(Remote.Source source, String download, String page) {
        String name = filename(download);
        return new Remote.Release(UNKNOWN_VERSION, download, page, null, null, name == null ? source.name() + ".jar" : name,
                name == null ? ReleaseType.UNKNOWN : ReleaseType.fromLabel(name));
    }
    static String filename(String url) {
        String path = URI.create(url).getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return Discovery.validFilename(name) ? name : null;
    }
}
