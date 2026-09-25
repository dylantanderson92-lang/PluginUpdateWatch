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
