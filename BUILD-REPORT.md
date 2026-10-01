# Build verification — 1.3.3

Checked 1 October 2026 (Pacific/Auckland).

- Final stable build: Java 21 Maven `clean verify` passed **95 tests**, zero failures, errors or skipped tests.
- Packaged descriptor declares version 1.3.3; bStats is bundled and relocated to `dev.updatewatch.lib.bstats`, with its MIT notice in `META-INF/BSTATS-LICENSE.txt`. No unrelocated `org/bstats/` classes are present.
- All six metrics lifecycle tests and existing provider, discovery, concurrency, integrity and cleanup tests pass.
- The final local stable JAR passed live checks on both documented Paper builds, including real Modrinth downloads, independent SHA-512 comparison, missing-checksum rejection, cleanup/backup and metrics opt-out/reload/shutdown checks.
- PR and release automation run the full suite on Java 21 and 25. The release workflow publishes its exact Java 21 artifact and matching SHA-256 file after both builds pass.
- README, wiki and release notes describe default-enabled bStats reporting, local/global opt-out, active-server count semantics and the 3–36 minute first dashboard delay.

Local stable JAR SHA-256: `10f2d24dee99b1230ef51618f997c6d054cc4ff98a0bad98f0f67c0cda2c2751`. CI packaging timestamps can produce a different archive hash; the release asset's own checksum is authoritative.

---

# Build verification — 1.3.3-SNAPSHOT (bStats)

Checked 1 October 2026 (Pacific/Auckland).

- Local Java 21 Maven `clean verify`: **95 tests, zero failures, errors or skipped tests**; shaded JAR packaging succeeded.
- Six new `MetricsControllerTest` tests cover the default for existing configs, explicit opt-out, rejection of malformed boolean values, no client creation while disabled, reload/disable/re-enable lifecycle without duplicate clients, and recovery after failed initialization.
- bStats Bukkit/base 3.2.1 are bundled and relocated to `dev.updatewatch.lib.bstats`. The packaged JAR includes the bStats MIT copyright/license notice. No separate bStats JAR is required.
- Registration was verified through the public bStats API: PluginUpdateWatch, ID 34400, owner dylyboo, Bukkit software ID 1.
- Isolated Paper 1.21.11 (Java 21) and Paper 26.3 (Java 25) checks verified the global opt-out, local opt-out, exactly one reporting thread when enabled, no additional thread on repeated reloads, and termination of the reporting thread after local opt-out. Update checks remained functional. See the live test report.
- Tests do not submit artificial usage to the public bStats dashboard. Successful public ingestion and dashboard counts are not claimed.
- This development build does not change `.github/release-version`; merging it does not automatically publish a stable release. Documentation describes the new telemetry default and opt-out, and records the required Spigot listing wording change before stable publication.

---

# Build verification — 1.3.2

Checked 30 September 2026 (Pacific/Auckland).

- Local Maven `clean verify` passed on Java 21: **89 tests, zero failures, errors or skipped tests**, followed by successful shaded JAR packaging.
- `ConfigCleanupTest` adds seven tests covering preview without mutation, exact backups, preservation of plugin files/data and settings, rejection of intervening config/file changes, case-insensitive existence checks, invalid rows/paths and unavailable directories.
- `DownloadFilenameTest` adds seven tests covering exact publisher names, plain fallback names, unsafe filename rejection, cross-plugin collision protection, safe same-plugin replacement, large descriptors and the descriptor size limit.
- Existing GitHub and Modrinth tests also assert that the selected provider filename is retained.
- Read-only verification against the supplied server's real EssentialsX descriptor (67,135 bytes) and mcMMO descriptor (110,525 bytes) passed both discovery and full JAR validation. Neither JAR was changed.
- CI runs the full Maven suite on Java 21 and Java 25. See [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for live command coverage and exact artifact evidence.

The added cleanup command only removes config rows after an explicit preview/confirmation and an exact backup. It does not infer old-to-new plugin identity, delete plugin data or automatically rewrite custom source URLs.

---

# Build verification — 1.3.1 and historical 1.3.0

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

## 1.3.1

Version 1.3.1 adds a safeguard for JAR filenames that differ only by case. It blocks ambiguous source selection across manual entries, discovery and legacy fallback, and adds three regression tests. These changes are not present in the published 1.3.0 JAR.

- Standard Maven `clean verify` passed locally using JDK 21.0.12: **75 tests, 0 failures, 0 errors, 0 skipped**, followed by JAR packaging and Gson shading.
- The final locally packaged 1.3.1 JAR passed live startup, scan/check/list/reload, real Modrinth downloads with independently verified SHA-512, and missing-checksum rejection on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. Both isolated servers stopped cleanly with exit status 0; see [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for artifact evidence.
- The normal Maven dependency remains Paper `1.21.11-R0.1-SNAPSHOT`. CI is configured to run `mvn clean verify` with this dependency on Java 21 and 25.
- All prior tests remain in the suite. Local HTTP tests inject deterministic responses and waits to cover failures without depending on remote rate limits or outages.

## Test coverage

| Test class | Published 1.3.0 | 1.3.1 | Coverage |
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
