# Live verification — published 1.3.0 and unreleased follow-up

Completed 27 September 2026 (Pacific/Auckland). The exact published 1.3.0 JAR and the locally packaged follow-up were each exercised on two real, isolated Paper servers with fresh test worlds and localhost-only ports. The follow-up includes the additional filename-collision safeguard and is not part of the published release, although its project version remains 1.3.0. The 26.3 runtime was copied from the supplied DylyCraft installation; its running server and worlds were not modified or stopped.

| Plugin artifact | Runtime | Startup | Scan/check/list/reload | Modrinth download + SHA-512 | Missing checksum rejection | Graceful stop |
| --- | --- | --- | --- | --- | --- | --- |
| Published 1.3.0 | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Published 1.3.0 | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |
| Unreleased follow-up | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Unreleased follow-up | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |

## Checks performed for each artifact and server

1. Started Paper with PluginUpdateWatch 1.3.0 and the original, unmodified LuckPerms 5.5.53 JAR. Confirmed both plugins enabled successfully.
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

Locally built unreleased follow-up JAR SHA-256:

```text
f1f0508e96d3c2419536d027110535f5bf67111644d239713e9809fa4b09a9f6
```

Downloaded LuckPerms 5.5.71 JAR SHA-512, equal in all four runs and equal to the provider's hash:

```text
188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27
```

The published artifact and local follow-up contain different source revisions. Passing results for the local follow-up were not used as a substitute for testing the published JAR; the complete procedure was run separately for each artifact.

## Scope

These are live server-console and real provider checks, not an in-game player/chat-click test, a performance soak, or a claim covering every intermediate Paper build. The Paper 26.3 build tested is an alpha. No downloaded update was installed into the user's active server. Injected checksum mismatch, HTTP 429/5xx retries, malformed archives and discovery ambiguity are covered by automated tests; these live runs did not alter a remote provider or deliberately corrupt its production files. The filename-collision fix is covered by three new follow-up tests. See [BUILD-REPORT.md](BUILD-REPORT.md) for the published 72-test suite and unreleased 75-test suite, and [COMPATIBILITY.md](COMPATIBILITY.md) for the supported range.
