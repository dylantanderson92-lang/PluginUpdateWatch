# Checksums & Download Security

Checksums are required by default for standard provider entries. The 1.4.0-SNAPSHOT preview adds a separate exception for explicitly configured web sources; see below.

```yaml
downloads:
  require-checksum: true
```

This default also applies when the setting is absent from an older configuration.

## Provider checksum support

### Modrinth

Modrinth supplies SHA-512 checksums. PluginUpdateWatch verifies them.

### GitHub

GitHub may provide SHA-256 digests for release assets. PluginUpdateWatch verifies a supplied digest when available.

### Spigot / Spiget

Spigot/Spiget generally does not provide a supported checksum.

With checksum enforcement enabled, an update can still be detected, but automatic download is blocked when no supported checksum is available.

## Allowing a missing checksum

To deliberately allow files without a checksum:

```yaml
downloads:
  require-checksum: false
```

Then run:

```text
/pu reload
```

The download is still subject to HTTPS and archive validation and is clearly reported as **unverified**.

!!! warning
    Setting `require-checksum: false` does not bypass a malformed or mismatched checksum. If a provider supplies a checksum and it does not match, the file is rejected.

A provider checksum detects corruption. It is not an independent publisher signature or malware scan.

## Web sources (1.4 preview)

`downloads.allow-unverified-web: true` defaults to allowing a missing checksum for explicit web source entries only. Warnings appear before and after downloading; plugin identity, JAR structure, filenames, HTTPS and size/time limits are still checked. Set this option to `false` with `downloads.require-checksum: true` to require checksums for web sources too. A supplied invalid or mismatched checksum always rejects the file. See [web sources](sources/web.md).
