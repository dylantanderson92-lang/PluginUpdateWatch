# Configuration

The configuration file is:

```text
plugins/PluginUpdateWatch/config.yml
```

The current default configuration is:

```yaml
check-interval-minutes: 360
notify-on-join: true
plugins-directory: ""
debug: false

metrics:
  enabled: true

network:
  connect-timeout-seconds: 10
  read-timeout-seconds: 15
  metadata-timeout-seconds: 30
  attempts: 3

downloads:
  max-size-mib: 100
  timeout-seconds: 120
  require-checksum: true

updates: []

plugins: {}
```

## Settings

### `check-interval-minutes`

How often update checks run.

Default:

```yaml
check-interval-minutes: 360
```

The minimum supported interval is 15 minutes.

### `notify-on-join`

Notify administrators when they join and updates are available.

```yaml
notify-on-join: true
```

### `plugins-directory`

```yaml
plugins-directory: ""
```

An empty value uses the parent of PluginUpdateWatch's data folder.

Relative paths use the server working directory.

### `debug`

```yaml
debug: false
```

When enabled, an additional scan summary is logged.

### Network settings

```yaml
network:
  connect-timeout-seconds: 10
  read-timeout-seconds: 15
  metadata-timeout-seconds: 30
  attempts: 3
```

Supported limits:

| Setting | Range |
| --- | --- |
| `connect-timeout-seconds` | 1–60 |
| `read-timeout-seconds` | 1–120 |
| `metadata-timeout-seconds` | 1–300 |
| `attempts` | 1–5 |

### Download settings

```yaml
downloads:
  max-size-mib: 100
  timeout-seconds: 120
  require-checksum: true
```

Supported limits:

| Setting | Range |
| --- | --- |
| `max-size-mib` | 1–1024 |
| `timeout-seconds` | 1–1800 |

## Usage metrics

Version 1.3.3 adds `metrics.enabled`, defaulting to `true`. Set it to `false` and run `/pu reload` to stop this plugin reporting. See [Usage Metrics](metrics.md) for the reported data and the global bStats opt-out. Older configs without this setting default to enabled; adding the section is optional unless you want to opt out.

## Applying changes

After editing the configuration, run:

```text
/pu reload
```
