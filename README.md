# PluginUpdateWatch

Detect installed Paper plugins, find update sources, notify admins, and download updates on request. Supports **Modrinth, Spigot and GitHub**.

**[Download from Releases](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/latest)** — choose `PluginUpdateWatch-1.2.0.jar` under Assets. Source code ZIP/TAR downloads are for developers, not server installation. You will be able to download from Modrinth shortly

## Install or upgrade

1. Stop the server.
2. Put `PluginUpdateWatch-1.2.0.jar` into the server's `plugins` folder. Remove the older PluginUpdateWatch JAR if upgrading. Keep its existing configuration folder.
3. Start the server. A scan begins automatically after startup.
4. Run `/pu list` as an operator to see updates or plugins needing a source link.

## Automatic detection

The scanner reads original JARs directly in the server's plugins folder and matches their name and version against installed plugins. It avoids Paper's remapped cache, which would produce different hashes. For plugins without settings, it tries an exact SHA-512 file lookup on Modrinth, then a recognized Modrinth, Spigot or GitHub link in the plugin's website metadata. Only the hash is sent to Modrinth; the JAR is not uploaded.

Detected sources are saved automatically. Unresolved plugins receive an entry with the correct filename and a blank source, plus a message in `/pu list`. The scanner never selects projects by similar names. Duplicate matching JARs are reported rather than guessed. PluginUpdateWatch excludes itself.

Names are trimmed and compared without case sensitivity; versions only have surrounding whitespace trimmed. Original metadata remains unchanged. Versions such as `1.0`, `01.0` and `1.0-beta` remain distinct. `/pu list` and console warnings identify duplicate candidate filenames, installed/JAR version mismatches, JARs with no matching installed plugin, and unreadable or incomplete descriptors. Discovery never picks one of several matching JARs. Existing explicit legacy sources can still be checked while such a warning is shown. Symbolic links are reported and skipped during directory scanning.

Existing links and legacy settings are preserved. If the config changes during a scan, results are discarded so edits are not overwritten. Saving can reformat YAML comments; existing setting values are retained.

## Manual entry: just filename and link

Open `plugins/PluginUpdateWatch/config.yml` and fill in the blank source:

```yaml
updates:
  - jar: "ExamplePlugin-2.0.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Use the exact installed filename, including `.jar`, and the real project's link. The example is a placeholder. No folder path, internal plugin name, resource ID or provider field is required. Keep one `updates:` section. Replace `updates: []` with the block when adding your first entry.

Spigot and GitHub work the same way:

```yaml
updates:
  - jar: "SpigotPlugin.jar"
    source: "https://www.spigotmc.org/resources/example-plugin.12345/"
  - jar: "GitHubPlugin.jar"
    source: "https://github.com/owner/repository"
```

Save and run `/pu reload`. A scan/check will run shortly. Run `/pu scan` to retry discovery any time. If a plugin's JAR filename changes during an upgrade, update its `jar` value. Unmatched old entries are retained and reported for review. Filename-based entries and discovery require original JARs directly in the plugins folder; legacy configured sources do not.

## Commands

| Command | Action |
| --- | --- |
| `/pu scan` | Detect installed JARs, find sources, save config, then check updates |
| `/pu check` | Check known sources without retrying automatic discovery |
| `/pu list` | Show cached updates and unresolved/configuration problems |
| `/pu download <plugin>` | Download an available update; tab completion supplies the plugin name |
| `/pu reload` | Reload config and schedule a new scan/check |
| `/pu stats` | Show the last check duration, update count and per-provider success/failure counts |

Permission: `pluginupdatewatch.admin`, granted to operators by default. Console commands omit `/`. The chat Download button fills the command; press Enter to submit it. Checks run every six hours by default (minimum configurable interval: 15 minutes). Network requests and hashing run off the server thread. Admins are notified when update results change and when joining with updates available.

## Installing downloaded updates

Downloads go to `plugins/PluginUpdateWatch/downloads/`. Review publisher compatibility notes, stop the server, replace the old plugin JAR with the download, then restart. Keep the plugin's data/configuration folder. Downloading does not overwrite running plugins or install automatically.

## Supported sources

- **Modrinth:** stable releases listed for the server Minecraft version and Paper, Spigot or Bukkit. The primary matching JAR is preferred, and its SHA-512 checksum is verified.
- **Spigot:** updates are checked using Spiget, which may lag behind Spigot. Premium or external resources show a manual download link.
- **GitHub:** latest stable release. Direct download works when exactly one JAR is present. Multiple JARs require manual selection on the release page. A GitHub release-asset URL can select that filename in the latest release; update the link if the asset filename changes. No API token is required.

Downloads must contain a Paper/Bukkit descriptor with the expected plugin name, a version and a main class file. Every archive entry is checked for size, CRC, unsafe paths and duplicates. Both descriptors are checked if present. This checks identity/format, not malware, publisher authenticity or every dependency. Numeric dotted versions are compared numerically; custom labels are shown as different releases requiring review. Source errors are never reported as up to date.

**Checksums are required by default**, including when the setting is absent from an older config. Modrinth supplies SHA-512 checksums. GitHub supplies SHA-256 digests for some assets, which are verified when available; it would be incorrect to say GitHub never provides checksums. Spigot/Spiget generally does not supply a supported checksum. Missing checksums block the automatic download with an explanation and a release-page link; update checking still works for all three providers.

To deliberately allow a file without a checksum, set `downloads.require-checksum: false` and run `/pu reload`. An existing explicit `false` is preserved. These downloads still require HTTPS and archive validation, and chat plus console label them **unverified** before and after downloading. A supplied malformed or mismatched checksum always rejects the file, even when this exception is enabled. A provider checksum detects corruption; it is not an independent publisher signature or malware scan.

## Network, downloads and diagnostics (1.3.0)

Existing configs can omit these options; defaults are applied. Invalid numeric limits reject startup/reload with a specific message.

```yaml
plugins-directory: "" # Empty = parent of this plugin's data folder
debug: false
network:
  connect-timeout-seconds: 10 # 1–60
  read-timeout-seconds: 15 # 1–120
  metadata-timeout-seconds: 30 # 1–300, elapsed request budget
  attempts: 3 # 1–5, includes the original attempt
downloads:
  max-size-mib: 100 # 1–1024
  timeout-seconds: 120 # 1–1800, elapsed request budget
  require-checksum: true # Set false explicitly only to allow missing checksums with warnings
```

Requests retry HTTP 408, 429 and all 5xx responses, plus connection timeouts/resets, using three total attempts by default. Backoff follows `250 ms × attempt`: the default two waits are 250 ms and 500 ms. The setting `network.attempts` remains supported. Short `Retry-After` delays (up to four seconds) override a shorter backoff when the request budget permits. Longer waits are reported to the admin and suppress further requests to that host during the current scan while the cooldown is active, rather than violating the provider's delay. A 429 without retry information gets the bounded retries first, then a 60-second cooldown if they are exhausted. GitHub's rate-reset epoch is recognized. Permanent denials and 404s are not retried. Interrupted or partial download bodies fail and their temporary files are removed; an entire download is not silently restarted.

Connect/read timeouts are clamped to the remaining request budget; elapsed time is checked between stages and body reads. JVM/OS DNS resolution is synchronous and may outlast that budget. Up to five redirects are accepted, with loop detection and validation on every destination. Only known GitHub, Modrinth and Spigot/Spiget API/CDN hosts are allowed; external download hosts require manual download. Resolved non-public addresses are rejected. No private API tokens or response bodies are logged.

Archive expansion is additionally capped at 1 GiB overall, 256 MiB per entry and 100,000 entries. These fixed validation ceilings apply even if the download size limit is raised. Output filenames are sanitized and include a suffix to avoid collisions. Checksums and archive validation finish before accepting a JAR in the downloads folder.

`debug: true` adds one scan summary to the console. `/pu stats` reports the most recently accepted check; these local diagnostics are not uploaded to a telemetry service. Provider errors are logged with the plugin/provider and HTTP status when available.

Messages separate `[ERROR] NETWORK_ERROR`, `PROVIDER_ERROR` and `CONFIG_ERROR` from successful `[INFO] CURRENT` and `NOT_CURRENT` results. Rate limits include a retry delay when known; config errors direct admins to fix the entry and reload. A provider with no compatible release reports unknown update status, never "up to date". Errors use red text, warnings yellow, and current results green. If a whole scan fails, `/pu list` labels any older results as cached.

All mutable command/result/lifecycle state is owned by the server thread. `UpdateCheckService` returns an immutable report from the I/O executor. Reload invalidates an old report and queues one fresh scan after the old operation finishes. Config files are parsed before use and saved through an atomic replacement; an intervening disk edit rejects the save. After editing config, use `/pu reload` before checking. Like other file compare-and-replace operations, this cannot lock out an external editor that writes at exactly the same instant.

## Existing settings and compatibility

The older `plugins:` format remains supported, including `enabled: false` and `asset-regex`. A new explicit jar/source entry takes precedence; a blank entry does not override legacy settings.

Targets Paper **1.21.11–26.3**, using Java 21 bytecode and no server internals. `api-version: '1.21.11'` deliberately declares the minimum Paper API, not a range or a maximum. Servers below that floor are unsupported. Newer/unknown versions receive a compatibility warning rather than an arbitrary upper-version block. Use the runtime required by your Paper release. Folia is not supported. See [COMPATIBILITY.md](COMPATIBILITY.md) and [BUILD-REPORT.md](BUILD-REPORT.md) for verification scope; targeted versions are not all live-tested versions.

### Tested Versions vs API Version

| Declaration or check | Meaning |
| --- | --- |
| `api-version: '1.21.11'` | Minimum Paper API; not an upper compatibility bound |
| Target Paper 1.21.11–26.3 | Intended runtime range; intermediate builds are not all individually tested |
| Paper 1.21.11 API + Java 21 | Source compilation and local unit tests |
| Cached Paper 26.3 build 6 alpha API + Java 25 | Source compilation and local unit tests against this specific API build |
| Live Paper startup, commands and remote downloads for 1.3.0 | Not yet verified; test on a server copy before production use |

Existing jar/source and legacy `plugins:` entries remain valid without migration. The deliberate safety change is that an **omitted** checksum setting now requires checksums; administrators wanting the earlier permissive behavior must explicitly opt out as described above. No config values are silently rewritten to opt out.

Build this branch with JDK 21+ and Maven 3.9+: `mvn clean verify`. Install `target/PluginUpdateWatch-1.3.0.jar`, not the `original-` JAR. Gson is bundled and relocated. The latest published release remains the version shown under Releases until a new tag is published.

Pull requests and main pushes build/test on Java 21 and 25. Actions stores the Java 21 JAR/checksum as the `plugin-java-21` artifact for 14 days, including development builds on main. To release, set matching versions in `pom.xml` and `plugin.yml`, merge the verified change, then push a tag such as `v1.3.0`. The tag workflow runs the same checks, verifies the tag/version/checksum, and publishes that run's exact Java 21 artifact with automatically generated release notes. It never uses the older committed `downloads/` binaries and does not overwrite an existing release. Manual workflow runs on branches verify only; they do not publish a release.

## License

PluginUpdateWatch's project code and documentation, including version 1.2.0, are available under the [MIT License](LICENSE). Third-party dependencies retain their own licenses and notices.
