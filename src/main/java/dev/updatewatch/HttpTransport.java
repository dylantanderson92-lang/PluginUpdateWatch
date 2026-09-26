package dev.updatewatch;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Bounded HTTPS transport; every redirect is validated before opening a connection. */
final class HttpTransport {
    interface Connector { HttpURLConnection connect(URI uri) throws IOException; }
    interface Sleeper { void sleep(long millis) throws InterruptedException; }
    private final Connector connector;
    private final Sleeper sleeper;
    private final Settings settings;
    private final Map<String, Long> cooldowns = new HashMap<>();
    HttpTransport(Settings settings) { this(settings, HttpTransport::connectPublic, Thread::sleep); }
    HttpTransport(Settings settings, Connector connector, Sleeper sleeper) {
        this.settings = settings; this.connector = connector; this.sleeper = sleeper;
    }
    private static final Set<String> HOSTS = Set.of("api.github.com", "github.com", "release-assets.githubusercontent.com",
            "objects.githubusercontent.com", "github-releases.githubusercontent.com", "api.modrinth.com", "cdn.modrinth.com",
            "api.spiget.org", "cdn.spiget.org", "spigotmc.org", "www.spigotmc.org");
    static URI safeUri(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT)))
                throw new IOException("URL rejected: expected an approved HTTPS provider or download host; use the release page for external downloads");
            return uri;
        } catch (IllegalArgumentException | NullPointerException e) { throw new IOException("Malformed remote URL", e); }
    }
    private static HttpURLConnection connectPublic(URI uri) throws IOException {
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            byte[] b = a.getAddress();
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                    || a.isMulticastAddress() || (a instanceof Inet6Address && (b[0] & 0xe0) != 0x20)
                    || (a instanceof Inet4Address && ((b[0] & 255) == 0 || (b[0] & 255) >= 224
                    || ((b[0] & 255) == 100 && (b[1] & 255) >= 64 && (b[1] & 255) <= 127))))
                throw new IOException("Non-public remote address rejected");
        }
        return (HttpURLConnection) uri.toURL().openConnection();
    }
    InputStream open(String url, int seconds) throws IOException {
        URI initial = safeUri(url);
        long cooldown = cooldowns.getOrDefault(initial.getHost(), 0L) - System.currentTimeMillis();
        if (cooldown > 0) throw new Remote.HttpError(429, initial.getHost(), (cooldown + 999) / 1000);
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        for (int attempt = 1; ; attempt++) {
            long delay = Math.min(4000L, 500L << (attempt - 1));
            try { return request(initial, deadline); }
            catch (Remote.HttpError error) {
                boolean rateLimited = error.code == 429 || (error.code == 403 && error.retryAfterSeconds > 0);
                if (rateLimited) {
                    long secondsToWait = error.retryAfterSeconds > 0 ? Math.min(error.retryAfterSeconds, 86400) : 60;
                    cooldowns.put(initial.getHost(), System.currentTimeMillis() + secondsToWait * 1000);
                    if (secondsToWait > 4 || attempt >= settings.attempts() || remaining(deadline) <= secondsToWait * 1000) throw error;
                    delay = secondsToWait * 1000;
                } else if (!retryable(error.code) || error.retryAfterSeconds > 0 || attempt >= settings.attempts()) throw error;
            } catch (SocketTimeoutException | ConnectException error) {
                if (attempt >= settings.attempts()) throw error;
            }
            if (remaining(deadline) <= delay) throw new SocketTimeoutException("Request deadline exceeded during retry backoff");
            try { sleeper.sleep(delay); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException("Request cancelled"); }
        }
    }
    static boolean retryable(int status) { return status == 408 || status == 500 || status == 502 || status == 503 || status == 504; }
    private InputStream request(URI uri, long deadline) throws IOException {
        Set<URI> visited = new HashSet<>();
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!visited.add(uri)) throw new IOException("Redirect loop detected");
            remaining(deadline);
            HttpURLConnection c = connector.connect(uri);
            boolean handedOff = false;
            try {
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(Math.min(settings.connectMillis(), remaining(deadline)));
                c.setReadTimeout(Math.min(settings.readMillis(), remaining(deadline)));
                c.setRequestProperty("User-Agent", "PluginUpdateWatch/1.3.0 (+https://github.com/dylantanderson92-lang/PluginUpdateWatch)");
                int status = c.getResponseCode();
                remaining(deadline);
                if (Set.of(301, 302, 303, 307, 308).contains(status)) {
                    String location = c.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect missing Location header");
                    try { uri = safeUri(uri.resolve(location).toString()); }
                    catch (IllegalArgumentException e) { throw new IOException("Malformed redirect", e); }
                    continue;
                }
                if (status != 200) throw new Remote.HttpError(status, uri.getHost(), retryAfter(c, Instant.now()));
                InputStream body = c.getInputStream();
                handedOff = true;
                return new FilterInputStream(body) {
                    private void prepare() throws IOException { c.setReadTimeout(Math.min(settings.readMillis(), remaining(deadline))); }
                    @Override public int read() throws IOException { prepare(); int n = in.read(); remaining(deadline); return n; }
                    @Override public int read(byte[] b, int off, int len) throws IOException { prepare(); int n = in.read(b, off, len); remaining(deadline); return n; }
                    @Override public void close() throws IOException { try { super.close(); } finally { c.disconnect(); } }
                };
            } finally { if (!handedOff) c.disconnect(); }
        }
        throw new IOException("Too many redirects (maximum 5)");
    }
    private static int remaining(long deadline) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Request cancelled");
        long millis = (deadline - System.nanoTime()) / 1_000_000;
        if (millis <= 0) throw new SocketTimeoutException("Request deadline exceeded");
        return (int) Math.min(Integer.MAX_VALUE, millis);
    }
    static long retryAfter(HttpURLConnection c, Instant now) {
        String value = c.getHeaderField("Retry-After");
        if (value != null) {
            try { return Math.max(1, Long.parseLong(value.trim())); } catch (NumberFormatException ignored) { }
            try { return Math.max(1, Duration.between(now, ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds()); }
            catch (RuntimeException ignored) { }
        }
        if ("0".equals(c.getHeaderField("X-RateLimit-Remaining"))) {
            try {
                long reset = Long.parseLong(c.getHeaderField("X-RateLimit-Reset"));
                return Math.max(1, c.getURL().getHost().equals("api.github.com") ? reset - now.getEpochSecond() : reset);
            } catch (RuntimeException ignored) { }
        }
        return 0;
    }
}
