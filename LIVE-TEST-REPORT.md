# Live verification — bStats development build

Checked 1 October 2026 (Pacific/Auckland), using the final local `PluginUpdateWatch-1.3.3-SNAPSHOT.jar`.

Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25 both passed:

- Plugin startup with bundled, relocated bStats 3.2.1.
- Shared bStats opt-out: no reporting thread when globally disabled, even with the new local setting absent.
- Plugin-specific opt-out: no reporting thread when `metrics.enabled: false`, even when globally enabled.
- Enabling metrics: exactly one bStats reporting thread.
- Repeated `/pu reload`: still exactly one reporting thread.
- Disabling metrics through config and `/pu reload`: reporting thread terminates.
- `/pu check` still completes after metrics opt-out.
- Normal server stop with the metrics client active: both server processes exit with code 0.

The checks used the existing isolated verification servers under `.work/live`, not the user's production server. The test JAR and metrics/config files were restored afterward. Tests finished before bStats' first scheduled submission; no public dashboard ingestion or installation count is claimed.

Artifact SHA-256: `170aadf346fb206510fdfd471512f3640acd2afbfaaf5b4f08b6874834e3c849`

---

# Live verification — 1.3.2

Completed 30 September 2026 (Pacific/Auckland), using isolated localhost-only Paper servers and dedicated test worlds.

| Runtime | Startup and commands | Exact publisher filename + SHA-512 | Cleanup preview/backup/confirmation | Disabled status and single console report | Shutdown |
| --- | --- | --- | --- | --- | --- |
| Paper 1.21.11 build 132 / Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Paper 26.3 build 41 alpha / Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |

The final local 1.3.2 JAR was tested on both servers. Commands covered startup discovery, `pu scan`, `pu check`, `pu list`, `pu download LuckPerms`, `pu cleanup`, `pu cleanup confirm` and `pu reload`.

- A real Modrinth download retained exactly `LuckPerms-Bukkit-5.5.71.jar`. Independent SHA-512 verification matched Modrinth metadata; the installed LuckPerms 5.5.53 JAR was unchanged.
- A seeded missing-file entry was reported once. Cleanup preview left config unchanged; confirmation removed only that entry and saved the complete original config in a backup before a successful fresh scan.
- A real Spigot check still reported an update, while downloading without a checksum remained blocked without creating/changing output files.
- With LuckPerms deliberately disabled, the report showed zero unresolved plugins and one intentionally disabled plugin. The disabled message appeared once with no warning prefix.
- The supplied server's EssentialsX and mcMMO JARs separately passed read-only discovery and full archive validation using the larger descriptor bound. No live-server config cleanup or installed-plugin replacement was performed on the user's server.

Final local 1.3.2 JAR SHA-256:

```text
a4b08fca648b07203051378d2cfa31196d424ba05e96122ab4aeb03788d37341
```

CI rebuilds the reviewed source and publishes its exact Java 21 artifact with its own checksum; the local test hash can differ due to build timestamps/toolchains. The 26.3 runtime is an alpha build. Console coverage does not establish in-game chat interaction, long-duration performance or compatibility with every intervening Paper build. Cleanup deliberately does not infer which new JAR an old missing filename belonged to.

---

# Live verification — 1.3.0 and 1.3.1

Completed 27 September 2026 (Pacific/Auckland). The exact published 1.3.0 JAR and the final locally packaged 1.3.1 JAR were each exercised on two real, isolated Paper servers with dedicated test worlds and localhost-only ports. Version 1.3.1 includes the additional filename-collision safeguard. The 26.3 runtime was copied from the supplied DylyCraft installation; the user's server and worlds were not modified or stopped.

| Plugin artifact | Runtime | Startup | Scan/check/list/reload | Modrinth download + SHA-512 | Missing checksum rejection | Graceful stop |
| --- | --- | --- | --- | --- | --- | --- |
| Published 1.3.0 | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Published 1.3.0 | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |
| Final local 1.3.1 | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Final local 1.3.1 | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |

## Checks performed for each artifact and server

1. Started Paper with the indicated PluginUpdateWatch version and the original, unmodified LuckPerms 5.5.53 JAR. Confirmed both plugins enabled successfully.
2. Used an older-style configuration without `downloads.require-checksum`. Automatic discovery identified LuckPerms by its published Modrinth file hash and persisted the correct source link.
3. Ran `pu scan`, `pu check` and `pu list` through the server console. Confirmed the compatible stable release `v5.5.71-bukkit` was shown. Because that publisher label contains a suffix, the plugin correctly displays `NOT_CURRENT` with a request to verify version ordering rather than asserting a numeric comparison.
4. Ran `pu download LuckPerms`. The real Modrinth download completed with the plugin's `Checksum verified` message. Independently computed SHA-512 of the downloaded file and compared it with the [Modrinth version API](https://api.modrinth.com/v2/version/b0mk8uS6).
5. Confirmed the installed LuckPerms JAR was unchanged; the downloaded update was stored only inside PluginUpdateWatch's downloads folder.
6. Changed LuckPerms' source to [Spigot resource 28140](https://www.spigotmc.org/resources/luckperms.28140/) and ran `pu reload`. The subsequent list still showed the update but explained that its missing checksum blocked automatic download.
7. Ran `pu download LuckPerms` again. Confirmed the actionable `PROVIDER_ERROR` / `Download blocked` message, no downloaded file added or changed, and no leftover temporary file. This also verified the strict default with the checksum setting absent.
8. Stopped each test server through its console and confirmed exit status 0.

## Artifact evidence

Published [PluginUpdateWatch 1.3.0 JAR](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/download/v1.3.0/PluginUpdateWatch-1.3.0.jar), from release commit `4410b43a3aff6b904c9906cc75a97a2b6cc836dc`, SHA-256:

```text
040847286752850f842df9966563742ebe3c5a36bcf10146c937a5f31516ae18
```

Final locally built 1.3.1 JAR SHA-256:

```text
26a2253deb19a654412f2dc71beab6ac4284a8a0b600ba54180c16313d0e00ac
```

Downloaded LuckPerms 5.5.71 JAR SHA-512, equal in all four runs and equal to the provider's hash:

```text
188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27
```

The complete procedure was run separately for each artifact. Release automation builds the reviewed 1.3.1 source again and publishes that run's exact Java 21 JAR and checksum. Toolchains and build timestamps can make its hash differ from the local pre-release JAR recorded here.

## Scope

These are live server-console and real provider checks, not an in-game player/chat-click test, a performance soak, or a claim covering every intermediate Paper build. The Paper 26.3 build tested is an alpha. No downloaded update was installed into the user's server. Injected checksum mismatch, HTTP 429/5xx retries, malformed archives and discovery ambiguity are covered by automated tests; these live runs did not alter a remote provider or deliberately corrupt its production files. The filename-collision fix is covered by three new regression tests. See [BUILD-REPORT.md](BUILD-REPORT.md) for the 1.3.0 72-test suite and 1.3.1 75-test suite, and [COMPATIBILITY.md](COMPATIBILITY.md) for the supported range.
