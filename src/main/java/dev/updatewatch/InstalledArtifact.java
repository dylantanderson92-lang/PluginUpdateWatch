package dev.updatewatch;

import java.io.InterruptedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;

/** Compare provider digests with the unambiguous original installed JAR, never Paper's remapped cache. */
final class InstalledArtifact {
    static boolean matches(Remote.Source source, Remote.Release release, List<Discovery.Jar> jars) throws Exception {
        if (!release.hasChecksum()) return false;
        var plugin = new Discovery.Installed(source.name(), source.installed(), null);
        var match = Discovery.matchReport(jars, plugin);
        if (match.jar() == null) return false;
        Path path = match.jar().path();
        var before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || !Discovery.metadataMatches(Discovery.read(path), plugin)) return false;
        Map<MessageDigest, byte[]> digests = new LinkedHashMap<>();
        add(digests, "SHA-512", release.sha512()); add(digests, "SHA-256", release.sha256());
        try (var in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Installed checksum comparison cancelled");
                for (var digest : digests.keySet()) digest.update(buffer, 0, n);
            }
        }
        var after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey()))
            throw new java.io.IOException("Installed JAR changed during checksum comparison; run a new check");
        return digests.entrySet().stream().allMatch(e -> MessageDigest.isEqual(e.getKey().digest(), e.getValue()));
    }
    private static void add(Map<MessageDigest, byte[]> digests, String algorithm, String expected) throws Exception {
        if (expected == null) return;
        var digest = MessageDigest.getInstance(algorithm);
        if (!expected.matches("[a-fA-F0-9]{" + digest.getDigestLength() * 2 + "}"))
            throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Provider supplied an invalid " + algorithm + " checksum");
        digests.put(digest, HexFormat.of().parseHex(expected));
    }
}
