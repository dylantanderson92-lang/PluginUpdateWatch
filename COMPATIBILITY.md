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

