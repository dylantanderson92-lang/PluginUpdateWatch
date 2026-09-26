# PluginUpdateWatch 1.3.0 maintenance candidate

- Retry HTTP 429 and all 5xx responses with three total attempts by default and 250/500 ms waits. Honor short Retry-After delays; defer longer waits with actionable messages and per-scan cooldowns.
- Require provider checksums by default. Modrinth provides SHA-512; GitHub may provide a SHA-256 asset digest; Spigot/Spiget generally has no supported digest. Missing checksums block downloads unless explicitly allowed in config.
- Preserve `downloads.require-checksum: false` as an explicit opt-out, with chat and console warnings before and after an unverified download. A supplied wrong checksum always rejects the file.
- Normalize discovery names by trimming and case folding, and version whitespace only. Report duplicate candidate filenames, version mismatches, uninstalled JARs and invalid descriptors. Never guess between ambiguous files.
- Separate network, provider and config failures from CURRENT/NOT_CURRENT results using actionable, color-coded messages. Failed whole scans label older results as cached.
- Keep the minimum API at 1.21.11 and document the target Paper 1.21.11–26.3 range separately from tested API/runtime combinations.
- Keep server-thread-owned state, immutable background reports, reload invalidation and atomic config saves.
- Validate archive CRCs, entry paths/size limits, descriptor identity/version and main-class presence before accepting downloads.
- Add PR/main CI on Java 21 and 25, development artifacts, and tag-triggered releases using the exact verified JAR and checksum with generated release notes.
- Retain configurable network/download limits, the optional plugin directory, local `/pu stats` and debug summaries.

**Upgrade note:** existing jar/source and legacy entries need no migration. If the checksum setting is absent, downloads without provider checksums are now blocked. To deliberately restore that earlier permissive behavior, add:

```yaml
downloads:
  require-checksum: false
```

Keep only one `downloads:` section and run `/pu reload`. This permits unverified downloads with warnings; it does not establish publisher authenticity. All three providers still support update checks.

Downloads require a deliberate command and manual installation after stopping the server. Filenames begin with `plugin-` and include a collision-resistant suffix. Unsupported external download hosts require manual download.

Validation: 72 tests passed on Java 21/Paper 1.21.11 API and Java 25/cached Paper 26.3 alpha API. Standard Maven clean verify and packaging passed on Java 21. Live Paper behavior has not been verified for this candidate.

---

## PluginUpdateWatch 1.2.0

Download **PluginUpdateWatch-1.2.0.jar** from Assets and put it in the server's plugins folder while the server is stopped. Replace the previous PluginUpdateWatch JAR and keep your configuration folder.

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

Local validation: 24 automated tests passed. Paper 1.21.11–26.3 remains the intended range; see COMPATIBILITY.md for exact API checks and limits. Live discovery on an actual Paper server has not been verified.
