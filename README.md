# PluginUpdateWatch

Detect installed Paper plugins, find update sources, notify admins, and download updates on request. Supports **Modrinth, Spigot, GitHub and explicit HTTPS web sources**.

**Stable release: 1.4.0.** The web-source support, quiet console reports and prerelease selection described below are now part of the standard release. The install/upgrade steps below use the published 1.4.0 JAR.

**[Download PluginUpdateWatch 1.4.0](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.4.0)** — choose `PluginUpdateWatch-1.4.0.jar` under Assets. Source code ZIP/TAR downloads remain available from the GitHub release page.

## Install or upgrade

1. Stop the server.
2. Put `PluginUpdateWatch-1.4.0.jar` into the server's `plugins` folder. Remove the older PluginUpdateWatch JAR if upgrading. Keep its existing configuration folder.
3. Start the server. A scan begins automatically after startup.
4. Run `/pu list` as an operator to see updates or plugins needing a source link.
