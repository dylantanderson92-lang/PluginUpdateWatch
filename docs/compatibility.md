# Compatibility

PluginUpdateWatch targets Paper:

```text
1.21.11â€“26.3
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

## 1.4.1 verification

The stable build passed 166 checks (165 unit tests and one packaged-JAR integration test) on both Java 21 and Java 25. The server owner confirmed successful live Paper 26.3 testing of the final preview. The stable artifact also passed isolated Paper 26.3 command verification. Earlier 1.21.11 artifact-specific evidence remains in the build report.

## 1.4.0 verification

The release passed Maven `clean verify` on Java 21 and Java 25. The final development regression suite contained **128 passing tests**, including the reported Plan/LuckPerms/DoubleDoors version aliases, notification eligibility and download freshness checks.

The corrected 1.4.0 development JAR passed isolated startup, scan/check/list/reload, web-download and strict-web-checksum checks on both Paper environments above. Upstream downloads were validated and saved, not installed or executed. These development checks are distinct from byte-for-byte live testing of the published stable JAR.

The published 1.4.0 JAR downloaded from Spigot matches the GitHub release SHA-256. See the [build report](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/BUILD-REPORT.md) for artifact evidence and the earlier development verification scope.

## 1.3.3 verification

The final local stable JAR passed the checks listed below on both documented Paper environments. It also passed bStats local/global opt-out, reload without duplicate clients, opt-out thread shutdown and normal server shutdown checks. The unit suite contains **95 tests**; PR/release CI runs on Java 21 and 25.

See the [build report](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/BUILD-REPORT.md) and [live test report](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/LIVE-TEST-REPORT.md).

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
