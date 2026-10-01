# PluginUpdateWatch 1.3.3

Download **PluginUpdateWatch-1.3.3.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Add the standard bStats client for PluginUpdateWatch (ID 34400), bundled and relocated to prevent dependency conflicts.
- Usage reporting defaults to enabled, including for older configurations. Set `metrics.enabled: false` in PluginUpdateWatch's config and run `/pu reload` to opt out. The shared bStats global opt-out is also respected.
- Report only standard bStats data; no installed-plugin list, source links, update results or custom charts are submitted.
- Gracefully handle malformed `metrics.enabled` config values with warnings; configuration typos no longer disable the plugin.
- Avoid duplicate metrics clients on reload and stop the reporting client when disabled. Metrics initialization failures are logged without disabling update checks.
- Document the public bStats dashboard/API, reported data, opt-out controls and reporting delay. Active reporting servers are not lifetime installations.

[Usage dashboard](https://bstats.org/plugin/bukkit/PluginUpdateWatch/34400) · [Metrics and opt-out documentation](https://dylantanderson92-lang.github.io/PluginUpdateWatch/metrics/)

The first dashboard data point can take 3–36 minutes after server startup. `/pu stats` continues to show local update-check diagnostics. Existing configurations remain compatible; no migration is required.

Validation: **95 unit tests**, with final-build live checks on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. See [BUILD-REPORT.md](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/BUILD-REPORT.md) and [LIVE-TEST-REPORT.md](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/LIVE-TEST-REPORT.md) for scope. Release automation tests both Java runtimes and publishes the verified Java 21 JAR and checksum.

---

# PluginUpdateWatch 1.3.2

Download **PluginUpdateWatch-1.3.2.jar** from Assets. Stop the server, replace the old PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Preserve the publisher's JAR filename for GitHub and Modrinth downloads. Providers without a filename use `<PluginName>.jar`. Remove the generated `plugin-` prefix and hash suffix. Existing file[...]
- Add `/pu cleanup` to preview obsolete config entries, and `/pu cleanup confirm` to remove them after saving an exact config backup. Only missing-file entries are removed; plugin JARs, downloaded[...]
- Increase the bounded plugin descriptor limit from 64 KiB to 1 MiB in both discovery and download validation, fixing rejection of the observed EssentialsX and mcMMO JARs.
- Display deliberately disabled plugins as informational entries and count them separately from unresolved plugins.
- Print each check report once in the console instead of logging the same warnings and results twice.

Existing two-field config entries remain supported. Download checksums and archive validation remain enforced. Existing downloads are not renamed or deleted. Missing compatible Modrinth releases a[...]

Validation: Maven `clean verify` passed **89 tests**, including 14 new cleanup, filename and descriptor regressions. Live verification results are recorded in [the live test report](https://github[...]

---

# PluginUpdateWatch 1.3.1

Download **PluginUpdateWatch-1.3.1.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep its configuration folder, then restart.

- Block source selection for on-disk filenames that differ only by case, such as `Alpha.jar` and `alpha.jar` on Linux. Manual entries, automatic discovery and legacy fallback all report the confli[...]
- Add three discovery regression tests, bringing the suite from 72 to **75 tests**.
- Update download links and record live Paper verification with artifact checksums.
- Publish the exact verified Java 21 artifact after Java 21/25 checks, using an explicit release marker or a matching tag, with curated notes and a generated changelog.

Existing configuration remains compatible; no migration is required from 1.3.0. Modrinth, GitHub and Spigot update checks remain supported. Provider checksums are still required for downloads by d[...]

Validation: local Maven `clean verify` passed all **75 tests** with zero failures, errors or skipped tests. The final local 1.3.1 JAR passed live startup, scan/check/list/reload, a real Modrinth u[...]

---

## PluginUpdateWatch 1.3.0

Download **PluginUpdateWatch-1.3.0.jar** from Assets. Stop your server, replace the older PluginUpdateWatch JAR, keep its configuration folder, then restart.

- Retry HTTP 429 and all 5xx responses with three total attempts by default and 250/500 ms waits. Honor short Retry-After delays; defer longer waits with actionable messages and per-scan cooldowns[...]
- Require provider checksums by default. Modrinth provides SHA-512; GitHub may provide a SHA-256 asset digest; Spigot/Spiget generally has no supported digest. Missing checksums block downloads un[...]
- Preserve `downloads.require-checksum: false` as an explicit opt-out, with chat and console warnings before and after an unverified download. A supplied wrong checksum always rejects the file.
- Normalize discovery names by trimming and case folding, and version whitespace only. Report duplicate candidate filenames, version mismatches, uninstalled JARs and invalid descriptors. Never gue[...]
- Separate network, provider and config failures from CURRENT/NOT_CURRENT results using actionable, color-coded messages. Failed whole scans label older results as cached.
- Keep the minimum API at 1.21.11 and document the target Paper 1.21.11–26.3 range separately from tested API/runtime combinations.
- Keep server-thread-owned state, immutable background reports, reload invalidation and atomic config saves.
- Validate archive CRCs, entry paths/size limits, descriptor identity/version and main-class presence before accepting downloads.
- Add PR/main CI on Java 21 and 25, development artifacts, and tag-triggered releases using the exact verified JAR and checksum with automatically generated release notes.
- Retain configurable network/download limits, the optional plugin directory, local `/pu stats` and debug summaries.

**Upgrade note:** existing jar/source and legacy entries need no migration. If the checksum setting is absent, downloads without provider checksums are now blocked. To deliberately restore that ea[...]

```yaml
downloads:
  require-checksum: false
```

Keep only one `downloads:` section and run `/pu reload`. This permits unverified downloads with warnings; it does not establish publisher authenticity. All three providers still support update che[...]

Downloads require a deliberate command and manual installation after stopping the server. Filenames begin with `plugin-` and include a collision-resistant suffix. Unsupported external download hos[...]

Validation: standard Maven `clean verify` and packaging passed with **72 tests** on both Java 21 and Java 25 in [release CI](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs[...]

---

## PluginUpdateWatch 1.2.0

Download **PluginUpdateWatch-1.2.0.jar** from Assets and put it in the server's plugins folder while the server is stopped. Replace the previous PluginUpdateWatch JAR and keep your configuration f[...]

- Detect installed plugins automatically at startup or with `/pu scan`.
- Identify exact published files through Modrinth SHA-512 lookup; fall back to a recognized source link in plugin website metadata.
- Save detected sources automatically and add blank entries for unresolved plugins.
- Manual entries require only `jar` (installed filename) and `source` (Modrinth, Spigot or GitHub link).
- Preserve legacy configuration and report duplicate JARs, missing files and invalid links.
- Continue to support update checks and downloads from **GitHub, Spigot and Modrinth**.

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

After editing config, run `/pu reload`. Updates download to `plugins/PluginUpdateWatch/downloads/`; install them during a restart.

Local validation: 24 automated tests passed. Paper 1.21.11–26.3 remains the intended range; see COMPATIBILITY.md for exact API checks and limits. Live discovery on an actual Paper server has not[...]
