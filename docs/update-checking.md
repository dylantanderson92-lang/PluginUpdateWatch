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

## Scheduled checks

The default check interval is:

```yaml
check-interval-minutes: 360
```

The minimum configurable interval is 15 minutes.
