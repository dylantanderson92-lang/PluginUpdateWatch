# 1.3.0 compatibility policy

The target remains Paper 1.21.11–26.3. `plugin.yml` declares **the minimum API version**, deliberately `1.21.11`; it is not an upper bound. Paper performs its own API compatibility check before enabling the plugin. `Compatibility.java` also classifies server versions at startup: known versions below the floor are rejected, and versions outside the target receive a warning instead of a speculative upper-version rejection. No claim is made for Folia or standalone Spigot.

The maintenance source was compiled locally against Paper `1.21.11-R0.1-SNAPSHOT` and cached `26.3.build.6-alpha`. Local tests run on Java 21 and Java 25 respectively. These checks do not simulate a live Paper server. Intermediate releases and every 26.3 build are not individually tested. CI builds against the declared 1.21.11 API on both Java runtimes.

Before production use, test startup, scan, reload during a scan, and download on a server copy. Confirm the provider lists the correct target plugin and, for Modrinth, a release explicitly tagged for your Minecraft version. GitHub/Spigot checks still require manual review of the publisher's server compatibility notes.

Historical verification follows; it is not a claim that newer source has been live-tested.

# 1.2.0 verification update

All updated source compiled against Paper 1.21.11 and the cached 26.3 build 6 alpha API. The 24 local tests passed on Java 25. The new discovery flow has not been tested on a live server. The evidence below describes 1.1.0 and does not extend its runtime verification to 1.2.0.

# Compatibility verification — PluginUpdateWatch 1.1.0

Checked 25 September 2026.

| Check | Paper 1.21.11 / Java 21 | Paper 26.3 / Java 25 |
| --- | --- | --- |
| Compile all plugin source against the Paper API | Passed | Passed |
| Automated tests | 14 passed, 0 failed | 14 passed, 0 failed |
| Load classes and resolve declared signatures from the distributed shaded JAR | Passed | Passed |
| Actual server startup and commands in this verification run | Not run | Not run |
| Earlier core features on a real server | Not confirmed | Confirmed by the user, before Modrinth support |
| Live Modrinth check/download on a server | Not verified | Not verified |

The exact APIs tested were `1.21.11-R0.1-SNAPSHOT` and `26.3.build.6-alpha`. The exact Java runtimes were 21.0.12 and 25.0.4.1. The 26.3 API result applies to that cached build; it is not a test of every 26.3 build. Intermediate Minecraft versions were not tested.

The 14 tests exercise version comparison, plugin JAR validation, Modrinth stable-release and Minecraft/loader filtering, primary/ambiguous JAR selection, asset patterns, request parameters, and checksum acceptance/rejection. These are local fixture-based tests; they do not contact Modrinth. Packaged class loading resolves class signatures but does not execute every method or emulate Paper startup.

Actual Paper server JARs were not available locally, and outbound network access prevented downloading them. Therefore these results establish API and local logic compatibility, not complete live-server compatibility. The user's report independently confirms earlier core functionality on Paper 26.3, excluding the new Modrinth feature.

## Remaining live check

On a test copy of the server, install 1.1.0 and configure an installed plugin with its correct Modrinth project slug. Run `pu reload`, `pu check`, and `pu list` in the console. Verify the latest version against the project's stable releases for the server's Minecraft version. If an update exists, run `pu download <plugin>` and confirm that its JAR is saved under `plugins/PluginUpdateWatch/downloads/` without replacing the installed plugin. With the latest version installed, expect no update. If the publisher has not listed a compatible stable release, expect an explicit message rather than an incompatible download. Check `logs/latest.log` for exceptions.

The plugin itself remains unchanged by this verification.
