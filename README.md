# PluginUpdateWatch

A Paper plugin that checks configured plugins for new releases, notifies admins, and downloads updates on request.

**[Download PluginUpdateWatch 1.1.0](https://github.com/dylantanderson92-lang/PluginUpdateWatch/raw/refs/heads/main/downloads/PluginUpdateWatch-1.1.0.jar)**

It supports **Modrinth, Spigot, and GitHub**. Tell it where each plugin is published once; it then checks automatically every six hours and alerts admins. Downloading an update saves a JAR on the server. You still install that JAR while the server is stopped.

## Quick setup with Modrinth

1. Stop your server. Put the downloaded **JAR** in the server's `plugins` folder. If you installed 1.0.0, replace its JAR. Keep your existing configuration folder. Start the server.
2. Open `plugins/PluginUpdateWatch/config.yml` in your server's file manager.
3. Replace the line `plugins: {}` with the following block (or add this entry under your existing `plugins:` section):

```yaml
plugins:
  YourPluginName:
    source: modrinth
    project: your-plugin-slug
```

Replace `YourPluginName` with the name shown by `/plugins`. Replace `your-plugin-slug` with the last part of that plugin's Modrinth project URL. For example, a URL ending in `/plugin/example-plugin` uses `project: example-plugin`. These are placeholders, not real plugin settings. Use spaces for indentation, and keep only one `plugins:` section.

4. Save the file. Run `/pu reload`, then `/pu check` as an operator. In the server console, omit the leading slash.
5. When an update appears, run `/pu download YourPluginName`. The file is saved in `plugins/PluginUpdateWatch/downloads/`. Stop the server, replace the old plugin JAR in `plugins/` with that file, and restart. Keep the plugin's data/configuration folder.

The server's Minecraft version is detected automatically. Only stable releases listed for that version and Paper, Spigot, or Bukkit are considered. Alpha/beta releases and Fabric/Forge-only builds are excluded. If no compatible stable release exists, the plugin reports that explicitly. Modrinth's primary matching JAR is preferred; ambiguous files require manual download or an `asset-regex` selecting the right file. Downloads are verified against Modrinth's SHA-512 checksum. Existing Spigot and GitHub settings continue to work.

## Install

1. Put `PluginUpdateWatch-1.1.0.jar` in your server's `plugins` folder and restart.
2. Edit `plugins/PluginUpdateWatch/config.yml`. Add a source for each plugin you want to track (use its exact name from `/plugins`).
3. Run `/pu reload`, then `/pu check`.

```yaml
check-interval-minutes: 360
notify-on-join: true
plugins:
  ExamplePlugin:
    source: spigot
    resource-id: 12345
  AnotherPlugin:
    source: github
    repository: owner/repository
    asset-regex: '^AnotherPlugin-.*\.jar$'
  YourPluginName:
    source: modrinth
    project: your-plugin-slug
```

These are placeholders: replace names and IDs with the real plugins you use. For Spigot, take the numeric ID from the resource page URL. For GitHub, use `owner/repository`; the asset expression must select exactly one JAR from the latest stable release. Multi-module projects need an expression specific to the installed plugin. Only public repositories are supported.

## Commands

| Command | Action |
| --- | --- |
| `/pu` or `/pu list` | Show cached versions, updates, errors and clickable actions |
| `/pu check` | Check all installed plugins with configured sources |
| `/pu download <plugin>` | Download the cached release into `plugins/PluginUpdateWatch/downloads` |
| `/pu reload` | Reload source configuration and reset cached results |

Permission: `pluginupdatewatch.admin` (operators by default). Console is supported. Clicking **Download** fills in the command; press Enter to submit. Checks run after startup and every six hours by default, off the server thread. Operators are notified when results change and when joining if updates are available. Disabled installed plugins can also be tracked; add `enabled: false` to a source entry to exclude it.

## Installing downloaded updates

Review the release page for Minecraft version and dependency compatibility. Back up the server, stop it, replace the original plugin JAR with the downloaded JAR, then start it again. Remove the old JAR so two versions are not present. The downloader does not overwrite running plugins, install automatically, or hot reload plugins.

Downloads are limited to 100 MiB and validated as a plugin JAR with the expected plugin name and a main class. Temporary files are cleaned up on failure. HTTPS, connection/read timeouts and a transfer deadline are used. This is an identity/format check, not a malware scan or compatibility guarantee. Configure only publishers you trust.

## Version support and limitations

See [the compatibility verification report](COMPATIBILITY.md) for the API checks, 14 passing tests on each endpoint API/Java combination, user-confirmed earlier behavior on Paper 26.3, and remaining live Modrinth verification.

Targets **Paper 1.21.11 through 26.3**, using the 1.21.11 API and Java 21 bytecode. Run each Paper version on the Java version it requires (Java 21 for 1.21.11; newer Paper releases may require Java 25). Folia is not supported. This is a compatibility target, not a claim of live testing on every release.

There is no universal update source in plugin metadata: unconfigured plugins are explicitly reported as untracked. GitHub/Spigot releases are not automatically filtered by Minecraft compatibility. Numeric dotted versions are ordered numerically; custom labels such as `build-42` or `1.2-RC1` are reported as different releases requiring review. Equal versions and locally newer numeric versions are not offered as updates. Build metadata after `+` is ignored.

Spiget may lag behind Spigot. Premium and externally hosted Spigot resources require manual downloads using the release-page link. GitHub releases without exactly one matching JAR also require manual downloads. HTTP errors and rate limits are shown as check failures; they are never reported as up to date. There are no bundled API tokens or private repository credentials.

## Build

Install JDK 21+ and Maven 3.9+, then run `mvn clean package`. The shaded plugin is `target/PluginUpdateWatch-1.1.0.jar`; do not install the `original-` JAR. Gson is bundled and relocated; the Paper API is provided by the server. Tests cover version comparisons and rejection of incorrect downloads.

API references: [Paper setup](https://docs.papermc.io/paper/dev/project-setup/), [GitHub releases](https://docs.github.com/en/rest/releases/releases), [Spiget API](https://github.com/SpiGetOrg/Documentation/blob/master/swagger.yml).
