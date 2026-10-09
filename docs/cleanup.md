# Cleaning Old Config Entries

When a plugin JAR is renamed after an update, an older config row may point to a filename that no longer exists.

PluginUpdateWatch does not guess which new JAR replaced an old filename.

## Preview cleanup

Run:

```text
/pu cleanup
```

This previews `updates:` entries whose configured files no longer exist.

## Confirm cleanup

After reviewing the preview:

```text
/pu cleanup confirm
```

PluginUpdateWatch:

1. Rechecks the config and missing-file list.
2. Creates a backup under:

```text
plugins/PluginUpdateWatch/backups/
```

3. Removes exactly the previewed missing-file entries.
4. Runs a fresh scan afterward.

## What cleanup does not remove

Cleanup does **not** delete:

- plugin JARs
- downloaded updates
- plugin data folders
- legacy `plugins:` settings

Existing files are retained even if a plugin is disabled, the descriptor is unreadable, or the file is a symbolic link.

!!! tip
    If a plugin's JAR was renamed and you want to keep its configured source, update the existing `jar:` value before running cleanup.
