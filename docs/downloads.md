# Downloading Updates

Use:

```text
/pu download <plugin>
```

Downloads are saved to:

```text
plugins/PluginUpdateWatch/downloads/
```

## Filenames

For GitHub and Modrinth, PluginUpdateWatch preserves the safe publisher asset filename.

When a provider does not supply a filename, normally Spigot, the fallback is:

```text
<PluginName>.jar
```

PluginUpdateWatch does not automatically rename or delete previously downloaded files.

## Installing a downloaded update

1. Review the publisher's compatibility notes.
2. Stop the server.
3. Replace the old plugin JAR with the downloaded update.
4. Keep the plugin's existing data/configuration folder.
5. Restart the server.

PluginUpdateWatch does not overwrite running plugins or automatically install downloaded updates.

## Validation

Downloaded JARs must contain a valid Paper/Bukkit descriptor with:

- the expected plugin name
- a version
- a main class
- the referenced main class file

Archive validation also checks entries for size, CRC, unsafe paths and duplicates.

## Download limits

Default settings:

```yaml
downloads:
  max-size-mib: 100
  timeout-seconds: 120
  require-checksum: true
```

Fixed archive validation limits also apply even when the configured download size is increased.
