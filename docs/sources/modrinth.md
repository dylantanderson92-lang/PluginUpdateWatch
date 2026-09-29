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

PluginUpdateWatch looks for stable releases listed for:

- the server's Minecraft version
- Paper, Spigot or Bukkit

The primary matching JAR is preferred.

## Downloads

Modrinth supplies SHA-512 checksums, which PluginUpdateWatch verifies.

For Modrinth downloads, PluginUpdateWatch preserves the safe publisher asset filename.
