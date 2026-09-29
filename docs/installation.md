# Installation

## Requirements

PluginUpdateWatch targets:

- **Paper 1.21.11–26.3**
- Java compatible with your Paper version
- PluginUpdateWatch itself uses **Java 21 bytecode**

Folia and standalone Spigot are not claimed as supported.

## Install

1. Stop the Minecraft server.
2. Download `PluginUpdateWatch-1.3.2.jar`.
3. Place the JAR in the server's `plugins` folder.
4. Start the server.
5. PluginUpdateWatch creates its configuration folder and begins a scan after startup.
6. Run:

```text
/pu list
```

Operators receive the `pluginupdatewatch.admin` permission by default.

## Upgrade

When upgrading PluginUpdateWatch:

1. Stop the server.
2. Remove the older PluginUpdateWatch JAR.
3. Put the new JAR in the `plugins` folder.
4. Keep the existing `plugins/PluginUpdateWatch/` configuration folder.
5. Start the server.

Existing two-field `jar` / `source` entries and older legacy `plugins:` entries remain supported.

!!! warning
    Do not install the Maven `original-` JAR. The installable artifact is the normal `PluginUpdateWatch-<version>.jar`.
