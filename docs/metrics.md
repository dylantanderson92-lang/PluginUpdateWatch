# Usage Metrics

PluginUpdateWatch 1.3.3 includes the standard bStats client, registered as plugin **34400**. Earlier stable versions do not report usage.

[Open the usage dashboard](https://bstats.org/plugin/bukkit/PluginUpdateWatch/34400)

## What the numbers mean

The server count measures active reporting servers. It is not a lifetime installation count, a list of server owners, or a download counter. Servers must run a build with this integration and keep metrics enabled to appear. Offline servers and servers that opt out are not counted as active.

## Data and controls

Metrics default to enabled, including for older configs without a `metrics` section. Only standard bStats data is sent: plugin/server versions, online-player count, online-mode setting, Java version, OS name/version/architecture, CPU core count and bStats' random server identifier. Aggregate charts are public.

No custom charts are added. Installed-plugin lists, source URLs, configuration contents, update results, filenames, player names and chat are not sent by PluginUpdateWatch.

To disable this plugin's metrics, add the following to `plugins/PluginUpdateWatch/config.yml`, then run `/pu reload`:

```yaml
metrics:
  enabled: false
```

The shared `plugins/bStats/config.yml` opt-out is also respected. Set `enabled: false` there and restart the server to disable bStats globally. A report already in flight may finish when disabling the local setting.

See [bStats' server-owner documentation](https://bstats.org/docs/server-owners) for details.

## Reporting delay and API

bStats normally waits approximately 3–6 minutes before the first report and sends subsequent reports about every 30 minutes. The website publishes at hh:00 and hh:30, so the first dashboard data point can take **3–36 minutes after startup**. See [bStats troubleshooting](https://bstats.org/docs/troubleshooting). PluginUpdateWatch does not force immediate submissions or send historical installation data.

The public, read-only API can be used for a separate report or dashboard:

- [Plugin details and chart definitions](https://bstats.org/api/v1/plugins/34400)
- [Chart registry](https://bstats.org/api/v1/plugins/34400/charts)
- [API documentation](https://bstats.org/docs/rest-api)

Select a chart ID from the registry and request `/api/v1/plugins/34400/charts/{chartId}/data`. No private API key is needed for these public reports.

`/pu stats` continues to show local update-check diagnostics; it does not query bStats.

## Troubleshooting

Use the server command `version PluginUpdateWatch` to confirm version 1.3.3 or later. `pu version` is not a supported command. Check that both the local and shared bStats settings permit reporting. For temporary diagnostics, bStats supports `logFailedRequests`, `logSentData` and `logResponseStatusText` in its shared config; restart after changing those settings. Turn verbose diagnostics off afterward.

A successful submission in the console and a public dashboard/API outage are separate events. An HTTP 502 while viewing public statistics does not prove that the plugin failed to submit. Avoid repeated restarts to force reports: bStats limits submissions to one per half-hour window.
