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

**In 1.4.0**, the plugin selects the newest published release, beta or alpha listed for:

- the server's Minecraft version
- Paper, Spigot or Bukkit

The primary matching JAR is preferred. Beta and alpha builds display a release-type warning. Minecraft-version and loader compatibility filters still apply; newer incompatible builds are not selected.

### 1.4.1 preview: enabled plugins

In `1.4.1-SNAPSHOT`, plugins currently enabled by Paper are compared with the newest listed Paper/Spigot/Bukkit release, including alpha/beta, even if it omits the server's Minecraft version. This also finds a newer release with missing version labels when an older release lists your version.

An equal or newer installed version is CURRENT and omitted from console reports. A confirmed newer release is an update. When its game-version label is missing, in-game listings and downloads show a compatibility warning. Startup confirms that the installed plugin enabled; it cannot prove full functionality or compatibility of the new download.

Disabled plugins keep the exact Minecraft-version filter. Loader filtering, checksums, JAR validation and conservative version comparison still apply. Empty release lists and failed requests remain errors, never CURRENT.

## Downloads

Modrinth supplies SHA-512 checksums, which PluginUpdateWatch verifies.

For Modrinth downloads, PluginUpdateWatch preserves the safe publisher asset filename.
