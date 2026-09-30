# Usage Metrics (Upcoming 1.3.3)

PluginUpdateWatch 1.3.2 does not include bStats. The upcoming 1.3.3 build adds the standard bStats client, registered as plugin **34400**.

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

bStats normally waits approximately 3–6 minutes before the first report and sends subsequent reports about every 30 minutes. Allow additional time for the dashboard to update. PluginUpdateWatch does not force immediate submissions or send historical installation data.

The public, read-only API can be used for a separate report or dashboard:

- [Plugin details and chart definitions](https://bstats.org/api/v1/plugins/34400)
- [Chart registry](https://bstats.org/api/v1/plugins/34400/charts)
- [API documentation](https://bstats.org/docs/rest-api)

Select a chart ID from the registry and request `/api/v1/plugins/34400/charts/{chartId}/data`. No private API key is needed for these public reports.

`/pu stats` now shows the local metrics state, but it cannot distinguish a shared bStats opt-out from server-side rate limiting until a submission is attempted.

## Release checklist

Before publishing a build with this integration, replace the Spigot listing's **No telemetry** claim with **Optional bStats usage metrics; server owners can opt out** and link this page. The published 1.3.2 JAR remains unchanged.
