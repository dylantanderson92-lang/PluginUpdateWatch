package dev.updatewatch;

import java.io.*;
import java.nio.file.*;

final class DownloadManager {
    interface Opener { InputStream open(String url, int seconds) throws IOException; }
    static Path download(Remote.Source source, Remote.Release release, Path folder, Settings settings, boolean requireChecksum, Opener opener) throws Exception {
        if (requireChecksum && !release.hasChecksum()) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, source.type() + " supplies no supported checksum; use the release page for a manual download, or explicitly set downloads.require-checksum: false");
        Files.createDirectories(folder);
        Path temp = Files.createTempFile(folder, ".download-", ".tmp");
        try {
            try (InputStream in = open(opener, release.download(), settings.downloadSeconds()); OutputStream out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[8192]; long total = 0; int n;
                while ((n = read(in, buffer)) != -1) {
                    total += n;
                    if (total > settings.downloadBytes()) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, source.type() + " download rejected at " + total + " bytes (limit " + settings.downloadBytes() + ")");
                    out.write(buffer, 0, n);
                }
            }
            Remote.verifyHash(temp, release, requireChecksum);
            Remote.validate(temp, source.name());
            String filename = "plugin-" + source.name().replaceAll("[^A-Za-z0-9_-]", "_") + "-" + release.version().replaceAll("[^A-Za-z0-9._-]", "_");
            filename = filename.substring(0, Math.min(filename.length(), 140));
            String suffix = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest((source.name() + "\n" + release.version()).getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 12);
            Path target = folder.resolve(filename + "-" + suffix + ".jar");
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            return target;
        } finally { Files.deleteIfExists(temp); }
    }
    private static InputStream open(Opener opener, String url, int seconds) throws IOException {
        try { return opener.open(url, seconds); }
        catch (Remote.HttpError | Failure.Problem e) { throw e; }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Could not open the download connection", e); }
    }
    private static int read(InputStream in, byte[] buffer) throws IOException {
        try { return in.read(buffer); }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Download interrupted before completion; temporary file discarded", e); }
    }
}
