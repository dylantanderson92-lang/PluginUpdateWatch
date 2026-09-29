# Adding Plugins Manually

If PluginUpdateWatch cannot discover a source automatically, add the plugin to `updates:` manually.

Only two fields are required:

- `jar`
- `source`

## Modrinth

```yaml
updates:
  - jar: "ExamplePlugin-2.0.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

## Spigot

```yaml
updates:
  - jar: "SpigotPlugin.jar"
    source: "https://www.spigotmc.org/resources/example-plugin.12345/"
```

## GitHub

```yaml
updates:
  - jar: "GitHubPlugin.jar"
    source: "https://github.com/owner/repository"
```

Use the **exact installed filename**, including `.jar`.

You do not need to add:

- a folder path
- the internal plugin name
- a resource ID field
- a provider field

Keep only one `updates:` section.

If the config currently contains:

```yaml
updates: []
```

replace it with your entries.

After saving:

```text
/pu reload
```

## Renamed JARs

If a plugin's filename changes after an update, update the corresponding `jar:` value.

Unmatched old entries are retained and reported so PluginUpdateWatch does not guess which plugin they belonged to.
