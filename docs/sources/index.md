# Update Sources

PluginUpdateWatch supports these update-source types:

- [Modrinth](modrinth.md)
- [Spigot / Spiget](spigot.md)
- [GitHub Releases](github.md)
- [HTTPS wiki/project pages and direct downloads](web.md) — 1.4.0-SNAPSHOT preview

A manual entry always uses the same basic structure:

```yaml
updates:
  - jar: "ExactInstalledFilename.jar"
    source: "https://provider.example/project"
```

PluginUpdateWatch can also automatically discover supported source links from plugin metadata and, for Modrinth, by exact SHA-512 file lookup.
