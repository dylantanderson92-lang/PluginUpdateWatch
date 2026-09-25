# PluginUpdateWatch

Detect installed Paper plugins, find update sources, notify admins, and download updates on request. Supports **Modrinth, Spigot and GitHub**.

**[Download from Releases](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/latest)** — choose `PluginUpdateWatch-1.2.0.jar` under Assets. Source code ZIP/TAR downloads are for developers, not server installation. You will be able to download from Modrinth shortly

## Install or upgrade

1. Stop the server.
2. Put `PluginUpdateWatch-1.2.0.jar` into the server's `plugins` folder. Remove the older PluginUpdateWatch JAR if upgrading. Keep its existing configuration folder.
3. Start the server. A scan begins automatically after startup.
4. Run `/pu list` as an operator to see updates or plugins needing a source link.

## Automatic detection

The scanner reads original JARs directly in the server's plugins folder and matches their name and version against installed plugins. It avoids Paper's remapped cache, which would produce different hashes. For plugins without settings, it tries an exact SHA-512 file lookup on Modrinth, then a recognized Modrinth, Spigot or GitHub link in the plugin's website metadata. Only the hash is sent to Modrinth; the JAR is not uploaded.

Detected sources are saved automatically. Unresolved plugins receive an entry with the correct filename and a blank source, plus a message in `/pu list`. The scanner never selects projects by similar names. Duplicate matching JARs are reported rather than guessed. PluginUpdateWatch excludes itself.

Existing links and legacy settings are preserved. If the config changes during a scan, results are discarded so edits are not overwritten. Saving can reformat YAML comments; existing setting values are retained.

## Manual entry: just filename and link

Open `plugins/PluginUpdateWatch/config.yml` and fill in the blank source:

```yaml
updates:
  - jar: "ExamplePlugin-2.0.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Use the exact installed filename, including `.jar`, and the real project's link. The example is a placeholder. No folder path, internal plugin name, resource ID or provider field is required. Keep one `updates:` section. Replace `updates: []` with the block when adding your first entry.

Spigot and GitHub work the same way:

```yaml
updates:
  - jar: "SpigotPlugin.jar"
    source: "https://www.spigotmc.org/resources/example-plugin.12345/"
  - jar: "GitHubPlugin.jar"
    source: "https://github.com/owner/repository"
```

Save and run `/pu reload`. A scan/check will run shortly. Run `/pu scan` to retry discovery any time. If a plugin's JAR filename changes during an upgrade, update its `jar` value. Unmatched old entries are retained and reported for review. Filename-based entries and discovery require original JARs directly in the plugins folder; legacy configured sources do not.

## Commands

| Command | Action |
| --- | --- |
| `/pu scan` | Detect installed JARs, find sources, save config, then check updates |
| `/pu check` | Check known sources without retrying automatic discovery |
| `/pu list` | Show cached updates and unresolved/configuration problems |
| `/pu download <plugin>` | Download an available update; tab completion supplies the plugin name |
| `/pu reload` | Reload config and schedule a new scan/check |

Permission: `pluginupdatewatch.admin`, granted to operators by default. Console commands omit `/`. The chat Download button fills the command; press Enter to submit it. Checks run every six hours by default (minimum configurable interval: 15 minutes). Network requests and hashing run off the server thread. Admins are notified when update results change and when joining with updates available.

## Installing downloaded updates

Downloads go to `plugins/PluginUpdateWatch/downloads/`. Review publisher compatibility notes, stop the server, replace the old plugin JAR with the download, then restart. Keep the plugin's data/configuration folder. Downloading does not overwrite running plugins or install automatically.

## Supported sources

- **Modrinth:** stable releases listed for the server Minecraft version and Paper, Spigot or Bukkit. The primary matching JAR is preferred, and its SHA-512 checksum is verified.
- **Spigot:** updates are checked using Spiget, which may lag behind Spigot. Premium or external resources show a manual download link.
- **GitHub:** latest stable release. Direct download works when exactly one JAR is present. Multiple JARs require manual selection on the release page. A GitHub release-asset URL can select that filename in the latest release; update the link if the asset filename changes. No API token is required.

Downloads must contain a Paper/Bukkit descriptor with the expected plugin name and main class. HTTPS, size limits (100 MiB) and timeouts are enforced. This checks identity/format, not malware or every dependency. Numeric dotted versions are compared numerically; custom labels are shown as different releases requiring review. Source errors are never reported as up to date.

## Existing settings and compatibility

The older `plugins:` format remains supported, including `enabled: false` and `asset-regex`. A new explicit jar/source entry takes precedence; a blank entry does not override legacy settings.

Targets Paper **1.21.11–26.3**, using Java 21 bytecode and no server internals. Use the runtime required by your Paper release. Folia is not supported. See [COMPATIBILITY.md](COMPATIBILITY.md) and [BUILD-REPORT.md](BUILD-REPORT.md) for verification scope.

Build with JDK 21+ and Maven 3.9+: `mvn clean package`. Install `target/PluginUpdateWatch-1.2.0.jar`, not the `original-` JAR. Gson is bundled and relocated. The release workflow builds/tests the source and attaches the distributed JAR to GitHub Releases.
