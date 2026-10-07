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

For GitHub and Modrinth, PluginUpdateWatch preserves the safe publisher asset filename. Direct web downloads preserve a safe filename from the URL, including spaces, `+` and build identifiers. Extensionless web endpoints use `<PluginName>.jar`.

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

## Freshness and inspection downloads

Version formatting alone does not establish an update. For example, Plan's `5.8 build 3638` and `5.8+build.3638` describe the same build, while platform labels such as `paper-1.4.9` can match an installed `1.4.9`.

When the installed original JAR can be matched, an identical downloaded file is rejected. Numerically older versions and same/older descriptor versions behind an advertised update are also rejected before the file is accepted.

An unknown web version or ambiguous version difference is labelled **Download for inspection**; it does not establish that a newer release exists or trigger an update notification. A changed snapshot with the same descriptor version may be saved for inspection, with an explicit warning that a newer version has not been established.

Alpha, beta, prerelease, development and unknown release types carry warnings. Review the publisher's compatibility notes before installing. Generic filenames such as `BigDoors.jar` can stay unchanged across genuine releases; the filename alone is not the version.

## Download limits

Default settings:

```yaml
downloads:
  max-size-mib: 100
  timeout-seconds: 120
  require-checksum: true
  allow-unverified-web: true
```

Explicit web sources may lack checksums and are allowed with warnings by default. See [Checksums & Security](checksums.md) to require them.

Fixed archive validation limits also apply even when the configured download size is increased.
