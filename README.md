[README.md](https://github.com/user-attachments/files/32298808/README.md)
# PluginUpdateWatch

A Paper plugin that checks configured plugins for new releases, notifies admins, and downloads updates on request.

## Install

1. Put `PluginUpdateWatch-1.0.0.jar` in your server's `plugins` folder and restart.
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

Targets **Paper 1.21.11 through 26.3**, using the 1.21.11 API and Java 21 bytecode. Run each Paper version on the Java version it requires (Java 21 for 1.21.11; newer Paper releases may require Java 25). Folia is not supported. This is a compatibility target, not a claim of live testing on every release.

There is no universal update source in plugin metadata: unconfigured plugins are explicitly reported as untracked. GitHub/Spigot releases are not automatically filtered by Minecraft compatibility. Numeric dotted versions are ordered numerically; custom labels such as `build-42` or `1.2-RC1` are reported as different releases requiring review. Equal versions and locally newer numeric versions are not offered as updates. Build metadata after `+` is ignored.

Spiget may lag behind Spigot. Premium and externally hosted Spigot resources require manual downloads using the release-page link. GitHub releases without exactly one matching JAR also require manual downloads. HTTP errors and rate limits are shown as check failures; they are never reported as up to date. There are no bundled API tokens or private repository credentials.

## Build

Install JDK 21+ and Maven 3.9+, then run `mvn clean package`. The shaded plugin is `target/PluginUpdateWatch-1.0.0.jar`; do not install the `original-` JAR. Gson is bundled and relocated; the Paper API is provided by the server. Tests cover version comparisons and rejection of incorrect downloads.

API references: [Paper setup](https://docs.papermc.io/paper/dev/project-setup/), [GitHub releases](https://docs.github.com/en/rest/releases/releases), [Spiget API](https://github.com/SpiGetOrg/Documentation/blob/master/swagger.yml).
