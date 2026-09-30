# PluginUpdateWatch 1.3.3 (unreleased)

- Add the standard bStats client for PluginUpdateWatch (ID 34400), bundled and relocated to prevent dependency conflicts.
- Usage reporting defaults to enabled, including for older configurations. Set `metrics.enabled: false` in PluginUpdateWatch's config and run `/pu reload` to opt out. The shared bStats global opt-out is also respected.
- Report only standard bStats data; no installed-plugin list, source links, update results or custom charts are submitted.
- Avoid duplicate clients on reload and stop the reporting client when disabled. Metrics initialization failures are logged without disabling update checks.
- Add explicit startup/reload diagnostics and `/pu stats` status output. Shared bStats opt-out and HTTP 429 rate limiting are identified as separate from plugin initialization failures.
- Document the public dashboard/API, reported data, opt-out controls and reporting delay. Active reporting servers are not lifetime installations.

This is a `1.3.3-SNAPSHOT` development build. The release marker is unchanged, so merging this PR does not publish a new GitHub release. Before a stable release, replace the Spigot listing's **No telemetry** claim with **Optional bStats usage metrics; server owners can opt out**.

# PluginUpdateWatch 1.3.2

Download **PluginUpdateWatch-1.3.2.jar** from Assets. Stop the server, replace the old PluginUpdateWatch JAR, keep the configuration folder, then restart.
