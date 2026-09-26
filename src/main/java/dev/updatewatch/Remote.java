package dev.updatewatch;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

final class Remote {
    static final class HttpError extends IOException {
        private static final long serialVersionUID = 1L;
        final int code;
        final long retryAfterSeconds;
        HttpError(int code) { this(code, "remote provider", 0); }
        HttpError(int code, String host, long retry) {
            super(host + ": HTTP " + code + (code == 429 || (code == 403 && retry > 0) ? " (rate limited)" : code == 403 ? " (access denied)" : "")
                    + (retry > 0 ? "; retry after " + retry + " seconds" : ""));
            this.code = code; retryAfterSeconds = retry;
        }
    }
    record Source(String name, String installed, String type, String id, String asset, String minecraft) {}
    record Release(String version, String download, String page, String sha512, String sha256) {
        Release(String version, String download, String page) { this(version, download, page, null, null); }
        Release(String version, String download, String page, String sha512) { this(version, download, page, sha512, null); }
        boolean hasChecksum() { return sha512 != null || sha256 != null; }
    }
    static JsonObject json(String url) throws IOException { return jsonValue(url, Settings.defaults()).getAsJsonObject(); }
    static JsonElement jsonValue(String url, Settings settings) throws IOException {
        return jsonValue(url, settings, new HttpTransport(settings));
    }
    static JsonElement jsonValue(String url, Settings settings, HttpTransport transport) throws IOException {
        try (InputStream in = transport.open(url, settings.metadataSeconds())) {
            byte[] bytes = in.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw new IOException("API response too large");
            try {
                JsonElement parsed = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                if (parsed.isJsonNull()) throw new IOException("Empty API response");
                return parsed;
            } catch (JsonParseException e) { throw new IOException("Malformed JSON response", e); }
        }
    }
    static Release latest(Source s, Settings settings) throws IOException { return Providers.latest(s, url -> jsonValue(url, settings)); }
    static Path download(Source source, Release release, Path folder, Settings settings, boolean requireChecksum) throws Exception {
        return DownloadManager.download(source, release, folder, settings, requireChecksum, new HttpTransport(settings)::open);
    }
    static void verifyHash(Path path, String expected) throws Exception { verifyHash(path, expected, "SHA-512"); }
    static void verifyHash(Path path, String expected, String algorithm) throws Exception {
        if (expected == null) return;
        var digest = MessageDigest.getInstance(algorithm);
        if (!expected.matches("[a-fA-F0-9]{" + digest.getDigestLength() * 2 + "}")) throw new IOException("Invalid " + algorithm + " checksum");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        if (!MessageDigest.isEqual(digest.digest(), HexFormat.of().parseHex(expected))) throw new IOException("Downloaded file checksum does not match provider (" + algorithm + ")");
    }
    static void validate(Path path, String expectedName) throws Exception { JarValidation.validate(path, expectedName); }
}
