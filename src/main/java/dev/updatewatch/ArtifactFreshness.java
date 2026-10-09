package dev.updatewatch;

import java.nio.file.*;

/** A valid plugin archive is not necessarily a new plugin version. Runs before accepting the download. */
final class ArtifactFreshness {
    static void validate(Remote.Source source, Remote.Release release, Path downloaded, Path installed) throws Exception {
        if (installed != null && Files.mismatch(installed, downloaded) == -1)
            throw Failure.problem(Failure.Kind.ALREADY_INSTALLED, "Source returned a byte-for-byte copy of the installed JAR; you already have this file and no replacement was saved");
        String actual = Discovery.read(downloaded).version();
        var comparison = Versions.compare(source.installed(), actual);
        boolean promisedUpdate = !SourcePage.UNKNOWN_VERSION.equals(release.version())
                && Versions.compare(source.installed(), release.version()) == Versions.Status.UPDATE;
        boolean sameLabel = Versions.normalize(source.installed()).equals(Versions.normalize(actual));
        if (comparison == Versions.Status.CURRENT && (promisedUpdate || !sameLabel))
            throw Failure.problem(Failure.Kind.NO_NEWER_ARTIFACT, "Downloaded JAR version " + actual + " is the same or older than installed "
                    + source.installed() + "; no replacement was saved");
    }
}
