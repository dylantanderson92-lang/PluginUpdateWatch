# Permissions

PluginUpdateWatch currently uses one administrative permission:

```text
pluginupdatewatch.admin
```

It defaults to:

```text
op
```

The permission allows administrators to:

- view update results
- scan plugins
- check update sources
- download plugin JARs
- preview and confirm config cleanup
- view statistics
- reload configuration

The permission is declared in `plugin.yml` as the permission for `/pluginupdates` and its `/pu` alias.
