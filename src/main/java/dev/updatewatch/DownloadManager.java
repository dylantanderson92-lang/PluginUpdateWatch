package dev.updatewatch;

import java.io.*;
import java.nio.file.*;

final class DownloadManager {
    interface Opener { InputStream open(String url, int seconds) throws IOException; }
    static Path download(Remote.Source source, Remote.Release release, Path folder, Settings settings, boolean requireChecksum, Opener opener) throws Exception {
        if (requireChecksum && !release.hasChecksum()) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, source.type() + " supplies no supported checksum; use the release page for a manual download, or explicitly set downloads.require-checksum: false");
        String filename = release.filename() == null ? source.name() + ".jar" : release.filename();
        if (!Discovery.validFilename(filename)) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Unsafe download filename; use the release page for a manual download");
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
            Path target = folder.resolve(filename);
            // A generic asset name such as plugin.jar must never overwrite another plugin's download.
            try (var files = Files.list(folder)) {
                for (Path existing : files.filter(p -> p.getFileName().toString().equalsIgnoreCase(filename)).toList()) {
                    if (!existing.getFileName().toString().equals(filename) || !Files.isRegularFile(existing, LinkOption.NOFOLLOW_LINKS))
                        throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Download filename conflicts with an existing file; move or rename " + existing.getFileName() + " first");
                    try { Remote.validate(existing, source.name()); }
                    catch (Exception e) { throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Existing " + filename + " could not be verified as the same plugin; move or rename it first", e); }
                }
            }
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
