# Spigot / Spiget

PluginUpdateWatch supports Spigot resource links, with update information checked through Spiget.

## Manual source

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://www.spigotmc.org/resources/example-plugin.12345/"
```

## Update checks

Spiget may lag behind Spigot.

Premium or externally hosted resources can require manual downloading from the publisher.

## Checksums

Spigot/Spiget generally does not supply a supported checksum.

With the default:

```yaml
downloads:
  require-checksum: true
```

PluginUpdateWatch can still check the resource for updates, but automatic download is blocked when no checksum is available.

To deliberately allow missing checksums:

```yaml
downloads:
  require-checksum: false
```

Run `/pu reload` after changing the setting.
