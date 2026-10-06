# GitHub Releases

PluginUpdateWatch supports GitHub repositories that publish releases.

## Manual source

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://github.com/owner/repository"
```

## Release selection

Stable 1.3.3 checks the latest stable release. **Starting with 1.4.0-SNAPSHOT**, the plugin selects the newest published release by `published_at` among the 100 most recent API entries, including prereleases and excluding drafts. Prereleases show a warning in reports and before downloading. The release-page link points to that exact tag.

## Direct downloads

Direct download works when exactly one JAR is present in the latest release.

If multiple JARs are present, manual selection on the release page may be required.

A GitHub release-asset URL can be used to select a specific filename in the latest release. If that asset filename changes, update the configured link.

## Checksums

GitHub can provide a SHA-256 digest for some release assets. PluginUpdateWatch verifies the digest when available.

No GitHub API token is required.

GitHub downloads preserve the safe publisher asset filename.
