# Troubleshooting

## A plugin shows "Source not identified"

Add its source manually in:

```text
plugins/PluginUpdateWatch/config.yml
```

Example:

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Then run:

```text
/pu reload
```

## A JAR filename changed

Update the existing `jar:` value in the config.

PluginUpdateWatch retains unmatched old entries instead of guessing which new filename replaced them.

## Duplicate candidate JARs

If more than one JAR could represent the same installed plugin, PluginUpdateWatch reports the ambiguity rather than selecting one.

Stop the server, move the duplicate JAR out of the plugins folder, restart, then run:

```text
/pu scan
```

Renaming a duplicate to another `.jar` filename in the same folder does not resolve duplicate plugin metadata. Keep only the intended copy in the plugins folder.

## Filenames differ only by case

Files such as:

```text
Alpha.jar
alpha.jar
```

are considered ambiguous.

Stop the server and inspect the conflicting files. If they are copies of the same plugin, move the unwanted copy out of the plugins folder. If they are different plugins, give them distinct filenames that differ by more than letter case and update any corresponding `jar:` entries in the config. Restart the server, then run `/pu scan`.

## Download blocked: checksum missing

This is expected when:

```yaml
downloads:
  require-checksum: true
```

and the provider does not supply a supported checksum.

Spigot/Spiget commonly falls into this category.

You can either download manually from the publisher or explicitly opt out:

```yaml
downloads:
  require-checksum: false
```

Then run `/pu reload`.

For explicit web sources, `downloads.allow-unverified-web: true` permits a missing checksum by default. If you set this to `false` with `require-checksum: true`, a web source without a checksum is also blocked. A supplied invalid or mismatched checksum is always rejected; do not disable validation to work around a mismatch.

## A download is identical, older, or only available for inspection

Version 1.4.0 normalizes known build/platform labels and only notifies for confirmed newer versions. Plan `5.8 build 3638` and `5.8+build.3638`, for example, are the same build.

Run `/pu scan` after installing or renaming a plugin JAR so the cached installed metadata is refreshed. If a download is rejected as identical or stale, review the source's release and the downloaded artifact it advertises. A fixed artifact URL may keep returning the same file; update it or use an appropriate project/download page. Repeating the download cannot make that file newer.

**Download for inspection** means version order is uncertain. A different snapshot with the same version label may contain changes, but the plugin does not claim it is newer. Generic filenames can also remain unchanged for genuine updates; review the descriptor version and publisher's release notes.

## Current plugins are missing from console output

This is intentional in 1.4.0 and later. Console/RCON reports hide current plugins and deliberately disabled informational entries. In-game `/pu list` still shows the full report. Updates, unknown web downloads, errors and configuration warnings remain visible.

## Config error

Correct the affected entry in `config.yml` and run:

```text
/pu reload
```

## Provider or network error

Retry later with:

```text
/pu check
```

Provider errors are not reported as "up to date".

## Old result marked cached

If a complete scan fails, `/pu list` can label older results as cached.

Run a new scan/check after the underlying problem is resolved.

## Need diagnostics

Run:

```text
/pu stats
```

You can also temporarily enable:

```yaml
debug: true
```

and then run `/pu reload`.
