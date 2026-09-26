package dev.updatewatch;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HttpTransportTest {
    static class Response extends HttpURLConnection {
        final int status; final Map<String,String> headers; boolean closed;
        Response(int status, Map<String,String> headers) throws Exception { super(URI.create("https://api.github.com/example").toURL()); this.status = status; this.headers = headers; }
        public void disconnect() { closed = true; }
        public boolean usingProxy() { return false; }
        public void connect() { }
        public int getResponseCode() { return status; }
        public String getHeaderField(String key) { return headers.get(key); }
        public InputStream getInputStream() { return new ByteArrayInputStream("{}".getBytes()); }
    }
    @Test void retriesTransientFailureAndClosesConnections() throws Exception {
        var bad = new Response(503, Map.of()); var ok = new Response(200, Map.of()); var calls = new AtomicInteger(); var delays = new ArrayList<Long>();
        var transport = new HttpTransport(Settings.defaults(), uri -> calls.getAndIncrement() == 0 ? bad : ok, delays::add);
        try (var in = transport.open("https://api.github.com/test", 30)) { assertEquals("{}", new String(in.readAllBytes())); }
        assertEquals(2, calls.get()); assertEquals(List.of(500L), delays); assertTrue(bad.closed); assertTrue(ok.closed);
    }
    @Test void longRateLimitIsNotRetriedAndOtherRequestsUseCooldown() throws Exception {
        var limited = new Response(429, Map.of("Retry-After", "120")); var calls = new AtomicInteger();
        var t = new HttpTransport(Settings.defaults(), uri -> { calls.incrementAndGet(); return limited; }, millis -> fail());
        var error = assertThrows(Remote.HttpError.class, () -> t.open("https://api.modrinth.com/test", 30));
        assertEquals(120, error.retryAfterSeconds); assertTrue(error.getMessage().contains("rate limited"));
        assertThrows(Remote.HttpError.class, () -> t.open("https://api.modrinth.com/another", 30)); assertEquals(1, calls.get());
    }
    @Test void retriesShortRateLimitAfterRequiredDelay() throws Exception {
        var limited = new Response(429, Map.of("Retry-After", "1")); var ok = new Response(200, Map.of()); var count = new AtomicInteger(); var waits = new ArrayList<Long>();
        var t = new HttpTransport(Settings.defaults(), uri -> count.getAndIncrement() == 0 ? limited : ok, waits::add);
        t.open("https://api.github.com/test", 30).close(); assertEquals(List.of(1000L), waits);
    }
    @Test void recognizesGithubRateResetAndNeverRetriesForbidden() throws Exception {
        var limited = new Response(403, Map.of("X-RateLimit-Remaining", "0", "X-RateLimit-Reset", "1120"));
        assertEquals(120, HttpTransport.retryAfter(limited, Instant.ofEpochSecond(1000)));
        var forbidden = new Response(403, Map.of()); var count = new AtomicInteger();
        var t = new HttpTransport(Settings.defaults(), uri -> { count.incrementAndGet(); return forbidden; }, millis -> fail());
        assertThrows(Remote.HttpError.class, () -> t.open("https://api.github.com/test", 30)); assertEquals(1, count.get());
    }
    @Test void retryExhaustionIsBounded() throws Exception {
        var bad = new Response(502, Map.of()); var count = new AtomicInteger();
        var t = new HttpTransport(Settings.defaults(), uri -> { count.incrementAndGet(); return bad; }, millis -> {});
        assertThrows(Remote.HttpError.class, () -> t.open("https://api.github.com/test", 30)); assertEquals(3, count.get());
    }
    @Test void redirectsCannotEscapeAllowlistOrLoop() throws Exception {
        for (String location : List.of("http://github.com/file", "https://127.0.0.1/file", "https://evil.test/file", "https://api.github.com/test")) {
            var redirect = new Response(302, Map.of("Location", location)); var count = new AtomicInteger();
            var t = new HttpTransport(Settings.defaults(), uri -> { count.incrementAndGet(); return redirect; }, millis -> fail());
            assertThrows(IOException.class, () -> t.open("https://api.github.com/test", 30)); assertEquals(1, count.get()); assertTrue(redirect.closed);
        }
    }
    @Test void rejectsMalformedAndUnexpectedUrls() {
        for (String url : List.of("garbage", "https://api.github.com:444/a", "https://user@api.github.com/a", "https://api.github.com.evil/a", "https://api.github.com/a#x"))
            assertThrows(IOException.class, () -> HttpTransport.safeUri(url));
    }
}
