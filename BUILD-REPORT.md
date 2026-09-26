# Build verification — published 1.3.0 and unreleased follow-up

Checked 27 September 2026 (Pacific/Auckland).

## Published 1.3.0

- [GitHub Release v1.3.0](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.3.0) identifies commit `4410b43a3aff6b904c9906cc75a97a2b6cc836dc`.
- All **72 tests** passed with Maven `clean verify` on Java 21 and Java 25 in [CI](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/36248215330). Release automation also passed and published the verified Java 21 JAR with its SHA-256 checksum.
- Standard local Maven `clean verify` previously passed using JDK 21.0.12, including javac compilation, all 72 tests, JAR packaging and Gson shading.
- The 72-test revision also passed on Java 25.0.4.1 against the cached Paper `26.3.build.6-alpha` API; production source was separately compiled against that API with Java 21 bytecode using Eclipse ECJ.
- The exact GitHub Release JAR passed live startup, scan/check/list/reload, real Modrinth downloads with independently verified SHA-512, and missing-checksum rejection on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. Both isolated servers stopped cleanly with exit status 0. See [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for artifact-specific results.

Published JAR SHA-256:

```text
040847286752850f842df9966563742ebe3c5a36bcf10146c937a5f31516ae18
```

## Unreleased follow-up

This branch adds a safeguard for JAR filenames that differ only by case. It blocks ambiguous source selection across manual entries, discovery and legacy fallback, and adds three regression tests. These changes are not present in the published 1.3.0 JAR. The project version remains 1.3.0 until a future release is prepared.

- Standard Maven `clean verify` passed locally using JDK 21.0.12: **75 tests, 0 failures, 0 errors, 0 skipped**, followed by JAR packaging and Gson shading.
- The locally packaged follow-up JAR passed live startup, scan/check/list/reload, real Modrinth downloads with independently verified SHA-512, and missing-checksum rejection on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. Both isolated servers stopped cleanly.
- The normal Maven dependency remains Paper `1.21.11-R0.1-SNAPSHOT`. CI is configured to run `mvn clean verify` with this dependency on Java 21 and 25.
- All prior tests remain in the suite. Local HTTP tests inject deterministic responses and waits to cover failures without depending on remote rate limits or outages.

## Test coverage

| Test class | Published 1.3.0 | Unreleased follow-up | Coverage |
| --- | ---: | ---: | --- |
| CheckStateTest | 2 | 2 | Reload invalidation, no overlapping checks, queued scans |
| DiscoveryTest | 15 | 18 | Configuration round trips, legacy sources, normalized matches, duplicate JAR ambiguity, mismatches, orphan files and invalid descriptors; follow-up adds case-colliding filenames across explicit/discovered/legacy sources |
| DownloadValidationTest | 5 | 5 | Paper/Bukkit descriptors, wrong plugin identity, non-plugin files and HTML |
| FailureTest | 7 | 7 | Network/provider/config categories, retry actions, malformed responses, unknown compatibility status, successful version states and redaction |
| HardeningTest | 14 | 14 | Source paths, config types/duplicates, safe filenames, limits, config preservation, GitHub digests, failed downloads and compatibility boundaries |
| HttpTransportTest | 14 | 14 | 429/all 5xx retries on each provider, 250/500 ms backoff, Retry-After delays, exhaustion, cooldowns, date rounding, overflow, cancellation and redirects |
| IntegrityPolicyTest | 6 | 6 | Required checksum defaults, explicit opt-out, missing hashes before network access, SHA-256/SHA-512 success, wrong hashes and artifact validation |
| ModrinthTest | 7 | 7 | Stable release and Minecraft/loader filtering, primary/ambiguous assets, URL parameters and checksums |
| VersionsTest | 2 | 2 | Version comparison |
| **Total** | **72** | **75** | **0 failures, 0 errors, 0 skipped in the verification runs described above** |

## Configuration and compatibility

Existing `updates` rows and legacy `plugins` entries remain supported. An existing explicit `downloads.require-checksum: false` remains supported; an omitted value requires a provider checksum in 1.3.0. This intentional safety change is documented in README and release notes. Opting out never bypasses a supplied mismatched checksum.

The API floor remains 1.21.11. Paper 1.21.11–26.3 is the target range, not a claim of live verification on every intermediate build. The live 26.3 test runtime is build 41 alpha; the earlier alternate-API compilation applies to cached build 6 alpha. Console checks do not cover player chat interactions, a long-running load test or every installed plugin combination.
