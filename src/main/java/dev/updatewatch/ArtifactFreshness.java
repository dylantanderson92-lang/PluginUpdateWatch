package dev.updatewatch;

import java.nio.file.*;

/** A valid plugin archive is not necessarily a new plugin version. Runs before accepting the download. */
final class ArtifactFreshness {
    static void validate(Remote.Source source, Remote.Release release, Path downloaded, Path installed) throws Exception {
        if (installed != null && Files.mismatch(installed, downloaded) == -1)
            throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Source returned a byte-for-byte copy of the installed JAR; no update was downloaded");
        String actual = Discovery.read(downloaded).version();
        var comparison = Versions.compare(source.installed(), actual);
        boolean promisedUpdate = !SourcePage.UNKNOWN_VERSION.equals(release.version())
                && Versions.compare(source.installed(), release.version()) == Versions.Status.UPDATE;
        boolean sameLabel = Versions.normalize(source.installed()).equals(Versions.normalize(actual));
        if (comparison == Versions.Status.CURRENT && (promisedUpdate || !sameLabel))
            throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Downloaded JAR is the same or an older version than installed; the advertised update was not accepted");
    }
}
