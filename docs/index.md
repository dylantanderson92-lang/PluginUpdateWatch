# PluginUpdateWatch

**PluginUpdateWatch** is a Paper plugin that detects installed plugins, checks supported update sources, notifies administrators when updates are available, and downloads updates on request.

Supported update sources:

- **Modrinth**
- **Spigot / Spiget**
- **GitHub Releases**

## Current version

**PluginUpdateWatch 1.3.3**

[Download PluginUpdateWatch](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/latest){ .md-button .md-button--primary }

[Download the 1.3.3 JAR](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/download/v1.3.3/PluginUpdateWatch-1.3.3.jar) or view the latest release above.

## Quick start

1. Stop your Paper server.
2. Place `PluginUpdateWatch-1.3.3.jar` in the server's `plugins` folder.
3. Start the server.
4. Run `/pu list` as an operator.
5. Use `/pu scan` to retry automatic source discovery at any time.

!!! note
    PluginUpdateWatch does not automatically replace running plugin JARs. Downloads are placed in `plugins/PluginUpdateWatch/downloads/` for you to review and install during a restart.

## Main features

- Automatic plugin JAR discovery
- Exact Modrinth SHA-512 lookup
- Modrinth, Spigot and GitHub update checks
- Manual source configuration using only a JAR filename and source link
- Config cleanup with backups
- Download checksum verification
- Download archive validation
- Admin join notifications
- Update statistics and diagnostics

## Compatibility

PluginUpdateWatch targets Paper **1.21.11–26.3** and uses Java 21 bytecode.

See [Compatibility](compatibility.md) for the exact verification scope.
