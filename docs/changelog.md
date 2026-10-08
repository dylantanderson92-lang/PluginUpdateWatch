# Changelog

## 1.4.1-SNAPSHOT (unreleased)

- Check enabled plugins against the newest listed Modrinth Paper/Spigot/Bukkit release when Minecraft-version labels are missing. Keep CURRENT reports quiet and warn before downloading an unlisted-compatibility release.
- Keep strict game-version filtering for disabled plugins and preserve errors for failed checks.
- Add blank lines between config entries during scans and cleanup, including existing unchanged entries.
- Restore bStats, Gson and jsoup relocation and test the final shaded JAR, including bStats' own relocation check.

## 1.4.0

[Release and downloads](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.4.0). Existing 1.3.x configurations remain supported; keep the configuration folder when upgrading.

- Fix false alerts for Plan numbered-build labels, LuckPerms' Bukkit suffix and DoubleDoors' Paper prefix.
- Notify only for confirmed newer versions; label ambiguous downloads as inspection only.
- Reject identical installed JARs and stale descriptor versions behind advertised updates.

- Suppress current and intentionally disabled plugin rows in console/RCON reports; keep full in-game listings.
- Add explicit HTTPS wiki/project pages, direct JAR URLs and extensionless download endpoints.
- Allow missing checksums for web sources with warnings and a separate strict-policy override.
- Select newest compatible Modrinth alpha/beta/release and newest published GitHub prerelease/release.
- Display release-type warnings before download and descriptor versions afterward.
- Add bounded HTML parsing, redirect/security and console filtering regression tests.

## 1.3.3

- Add optional bStats usage metrics (plugin ID 34400), with a local `metrics.enabled` opt-out and support for the shared bStats opt-out.
- Stop the metrics client on disable and avoid duplicate clients on reload.
- Document public usage reports, reporting delays and the difference between active servers and lifetime installations.


## 1.3.2


Highlights:

- Preserve publisher JAR filenames for GitHub and Modrinth downloads.
- Use `<PluginName>.jar` when a provider supplies no filename.
- Add `/pu cleanup` and `/pu cleanup confirm`.
- Back up the config before confirmed cleanup.
- Increase the plugin descriptor limit from 64 KiB to 1 MiB.
- Show deliberately disabled plugins as informational entries.
- Avoid duplicate console reporting for each check.

Validation documented for this release includes **89 Maven tests** plus live verification on the project's two documented Paper test environments.

## 1.3.1

Highlights:

- Added protection against JAR filenames differing only by letter case.
- Added discovery regression tests.
- Retained checksum enforcement and existing configuration compatibility.

## 1.3.0

Highlights:

- Added bounded network retries.
- Required provider checksums by default.
- Added configurable network and download limits.
- Improved discovery ambiguity handling.
- Improved error classification and reporting.
- Added archive validation hardening.
- Added Java 21 / 25 CI coverage.

## 1.2.0

Highlights:

- Added automatic installed-plugin detection.
- Added exact Modrinth SHA-512 lookup.
- Added simple `jar` / `source` manual configuration.
- Continued support for GitHub, Spigot and Modrinth.
