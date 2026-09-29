# Compatibility

PluginUpdateWatch targets Paper:

```text
1.21.11–26.3
```

Its `plugin.yml` declares:

```yaml
api-version: '1.21.11'
```

This is the **minimum API version**, not an upper compatibility limit.

## Java

PluginUpdateWatch uses **Java 21 bytecode**.

Run the Java version required by your Paper build.

The documented live test environments include:

| Paper | Java |
| --- | --- |
| Paper 1.21.11 build 132 | Java 21 |
| Paper 26.3 build 41 alpha | Java 25 |

## 1.3.2 verification

The final local 1.3.2 JAR was documented as passing:

- startup
- scan
- check
- list
- reload
- real Modrinth download
- SHA-512 verification
- missing-checksum rejection
- cleanup preview/confirmation/backup
- disabled-plugin informational reporting

on both documented Paper test environments.

## Scope limits

This does not establish compatibility with:

- every intermediate Paper build
- every plugin combination
- future Paper builds
- sustained production load

The tested Paper 26.3 build is an alpha.

No support claim is made for:

- Folia
- standalone Spigot

For production use, test the plugin on a server copy before deploying changes.
