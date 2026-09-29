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
