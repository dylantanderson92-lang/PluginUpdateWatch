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
# 1.3.0 maintenance candidate

- Add PR/main CI on Java 21 and 25, development artifacts, and tag-triggered releases using the exact verified JAR and checksum with generated release notes.
- Extract check service, lifecycle state, config persistence, provider selection, HTTP transport and download/archive validation.
- Keep plugin state on the server thread; reject stale results after reload and queue a replacement scan.
- Validate config rows, filenames, repository IDs and source paths before use; preserve invalid/edited config files.
- Add configurable connect/read/request/download limits and plugin directory, bounded retries, rate-limit handling, redirect allowlist and failure logging.
- Verify GitHub API SHA-256 digests and Modrinth SHA-512; add an optional checksum-required policy.
- Validate archive CRCs, entry paths and size limits, descriptor identity/version and main class presence before accepting downloads.
- Add local `/pu stats` and debug scan summaries.
- Document the API floor separately from the targeted runtime range. Retain Paper 1.21.11 minimum API.

Existing configs use defaults for the new settings. Downloads still require a deliberate command and manual installation after stopping the server. Download filenames now begin with `plugin-` and include a collision-resistant suffix. Unsupported external download hosts require manual download.
