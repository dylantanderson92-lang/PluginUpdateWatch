# Build verification — 1.3.0 maintenance candidate

Checked 26 September 2026.

## Results

- Standard Maven `clean verify` passed using JDK 21.0.12, including javac compilation, all 72 tests, JAR packaging and Gson shading.
- All 72 tests also passed on Java 25.0.4.1 against the cached Paper `26.3.build.6-alpha` API; production source was separately compiled against that API with Java 21 bytecode using Eclipse ECJ.
- The normal Maven dependency remains Paper `1.21.11-R0.1-SNAPSHOT`. CI runs `mvn clean verify` with this dependency on Java 21 and 25.
- All 47 prior maintenance tests remain in the suite. Two existing fixtures were adjusted for the requested 250 ms first retry delay and the structured HTTP error type.
- No live Paper server startup, commands, remote provider requests or downloads were tested. Local HTTP tests inject deterministic responses and waits.

## Test coverage

| Test class | Tests | Coverage |
| --- | ---: | --- |
| CheckStateTest | 2 | Reload invalidation, no overlapping checks, queued scans |
| DiscoveryTest | 15 | Configuration round trips, legacy sources, normalized matches, duplicate JAR ambiguity, mismatches, orphan files and invalid descriptors |
| DownloadValidationTest | 5 | Paper/Bukkit descriptors, wrong plugin identity, non-plugin files and HTML |
| FailureTest | 7 | Network/provider/config categories, retry actions, malformed responses, unknown compatibility status, successful version states and redaction |
| HardeningTest | 14 | Source paths, config types/duplicates, safe filenames, limits, config preservation, GitHub digests, failed downloads and compatibility boundaries |
| HttpTransportTest | 14 | 429/all 5xx retries on each provider, 250/500 ms backoff, Retry-After delays, exhaustion, cooldowns, date rounding, overflow, cancellation and redirects |
| IntegrityPolicyTest | 6 | Required checksum defaults, explicit opt-out, missing hashes before network access, SHA-256/SHA-512 success, wrong hashes and artifact validation |
| ModrinthTest | 7 | Stable release and Minecraft/loader filtering, primary/ambiguous assets, URL parameters and checksums |
| VersionsTest | 2 | Version comparison |
| **Total** | **72** | **0 failures, 0 errors, 0 skipped** |

## Configuration and compatibility

Existing `updates` rows and legacy `plugins` entries remain supported. An existing explicit `downloads.require-checksum: false` remains supported; an omitted value now requires a provider checksum. This intentional safety change is documented in README and release notes. Opting out never bypasses a supplied mismatched checksum.

The API floor remains 1.21.11. Paper 1.21.11–26.3 is the target range, not a claim of live verification on every intermediate build. The 26.3 result applies to the cached alpha API above. The new retry, discovery and integrity behavior should be exercised on a test server before production rollout.
