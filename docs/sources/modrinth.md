# Modrinth

PluginUpdateWatch supports Modrinth plugin projects.

## Manual source

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

## Automatic discovery

For plugins without an explicit source, PluginUpdateWatch can calculate the original JAR's SHA-512 hash and perform an exact Modrinth file lookup.

Only the hash is sent. The JAR is not uploaded.

## Version selection

**In 1.4.0 and later**, the plugin selects the newest published release, beta or alpha listed for:

- the server's Minecraft version
- Paper, Spigot or Bukkit

The primary matching JAR is preferred. Beta and alpha builds display a release-type warning. Minecraft-version and loader compatibility filters still apply; newer incompatible builds are not selected.

## Downloads

Modrinth supplies SHA-512 checksums, which PluginUpdateWatch verifies.

For Modrinth downloads, PluginUpdateWatch preserves the safe publisher asset filename.
