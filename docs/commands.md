# Commands

All commands use `/pu`, which is an alias of `/pluginupdates`.

| Command | Description |
| --- | --- |
| `/pu list` | Show cached update results, unresolved plugins and configuration problems |
| `/pu scan` | Scan installed JARs, discover sources, save config and check updates |
| `/pu check` | Check already-known sources without retrying automatic discovery |
| `/pu download <plugin>` | Download an available update |
| `/pu cleanup` | Preview config entries whose configured JAR files are missing |
| `/pu cleanup confirm` | Back up the config and remove exactly the previewed missing-file entries |
| `/pu stats` | Show the latest check duration, update count and provider success/failure counts |
| `/pu reload` | Reload the configuration and schedule a fresh scan/check |

## Permission

All administrative commands use:

```text
pluginupdatewatch.admin
```

The permission defaults to server operators.

## Console

Console commands omit the slash:

```text
pu list
pu scan
pu check
```

## Download tab completion

For `/pu download`, tab completion supplies plugin names when an available update can be downloaded.
