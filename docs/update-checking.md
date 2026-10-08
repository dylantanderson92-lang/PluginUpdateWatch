# Finding Updates

PluginUpdateWatch checks configured sources and can also discover sources automatically.

## Automatic discovery

The scanner reads the original JAR files directly from the server's plugins directory.

For plugins without an explicit source, it tries:

1. An exact **SHA-512 file lookup on Modrinth**.
2. A recognized **Modrinth**, **Spigot** or **GitHub** link in the plugin's website metadata.

Only the file hash is sent to Modrinth. The plugin JAR itself is not uploaded.

## Safety rules

PluginUpdateWatch does not select projects merely because their names look similar.

It reports rather than guesses when it finds:

- multiple matching JARs
- filenames that differ only by case
- installed/JAR version mismatches
- JARs with no matching installed plugin
- unreadable or incomplete descriptors
- symbolic links during directory scanning

## Source errors

A provider error is not treated as "up to date".

When no compatible release is found, update status is reported as unknown and the release/source should be reviewed.

## Results and notifications

Only a confirmed newer version triggers update/join notifications. Equivalent version formats are treated as current; an ambiguous difference or an unknown web version does not prove that an update exists. Such downloads may be offered for inspection instead. See [Downloading Updates](downloads.md).

In 1.4.0 and later, console/RCON reports omit current plugins and intentionally disabled informational entries. Updates, unknown web downloads, failed checks and configuration warnings remain visible. In-game `/pu list` retains the full report; a quiet console does not mean configured plugins were skipped.

Modrinth and GitHub include prereleases in latest-release selection and warn about their type. Stable 1.4.0 requires matching Minecraft and Paper/Spigot/Bukkit labels on Modrinth. The [1.4.1 preview policy](sources/modrinth.md#141-preview-enabled-plugins) allows missing Minecraft labels for enabled plugins, with compatibility warnings; it retains loader filtering. [Web sources](sources/web.md) must be configured explicitly and cannot always establish a version or compatibility.

## Scheduled checks

The default check interval is:

```yaml
check-interval-minutes: 360
```

The minimum configurable interval is 15 minutes.
