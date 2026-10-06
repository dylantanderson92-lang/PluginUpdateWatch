# PluginUpdateWatch 1.4.0

Download **PluginUpdateWatch-1.4.0.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Finalize the 1.4 preview as a stable release with explicit HTTPS web sources, quiet console output and prerelease selection support.
- Preserve config compatibility for existing `jar/source` entries, legacy `plugins:` entries and older checksum settings while keeping stricter safety checks for modern downloads.
- Keep Paper 1.21.11–26.3 compatibility and Java 21/25 verification as part of the standard release validation.
- No migration is required from 1.3.x, but administrators should review the config if they rely on older defaults or explicit web-source settings.

Validation: Maven `clean verify` passed on Java 21 and Java 25, and the exact verified release artifact is published with checksum verification and generated release notes.

---

# PluginUpdateWatch 1.3.3

Download **PluginUpdateWatch-1.3.3.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Add the standard bStats client for PluginUpdateWatch (ID 34400), bundled and relocated to prevent dependency conflicts.
- Usage reporting defaults to enabled, including for older configurations. Set `metrics.enabled: false` in PluginUpdateWatch's config and run `/pu reload` to opt out. The shared bStats global opt-o[...]
- Report only standard bStats data; no installed-plugin list, source links, update results or custom charts are submitted.
- Validate `metrics.enabled` as a YAML boolean. Invalid values reject configuration loading rather than silently enabling reporting; use unquoted `true` or `false`.
- Avoid duplicate metrics clients on reload and stop the reporting client when disabled. Metrics initialization failures are logged without disabling update checks.
- Document the public bStats dashboard/API, reported data, opt-out controls and reporting delay. Active reporting servers are not lifetime installations.

[Usage dashboard](https://bstats.org/plugin/bukkit/PluginUpdateWatch/34400) · [Metrics and opt-out documentation](https://dylantanderson92-lang.github.io/PluginUpdateWatch/metrics/)

The first dashboard data point can take 3–36 minutes after server startup. `/pu stats` continues to show local update-check diagnostics. Existing configurations remain compatible; no migration i[...]

Validation: **95 unit tests**, with exact published-JAR live checks on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. See [BUILD-REPORT.md](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/BUILD-REPORT.md)

---
