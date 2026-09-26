# Build verification — 1.2.0

- Compiled all source against Paper 1.21.11 and cached Paper 26.3 build 6 alpha APIs, targeting Java 21 bytecode.
- All 24 automated tests passed: 10 discovery/config/link tests, 7 Modrinth tests, 5 JAR validation tests and 2 version-comparison tests.
- Discovery tests cover hash matching, saved configuration round trips, unresolved sources, legacy/disabled settings, duplicate JARs, metadata fallback, network errors, duplicate config entries, filename/link validation and checks without discovery.
- Compiled locally with Eclipse ECJ 3.41.0; tests and shading run with Maven. Gson is relocated.
- The new discovery flow has not been run inside a live Paper server. API compilation and fixture-based tests do not establish full runtime compatibility.
- The GitHub release workflow independently builds/tests with Maven on Java 21 and 25 before attaching the distributed JAR. Workflow status is the source of truth for those remote checks.
# 1.3.0 maintenance verification — 26 September 2026

- 47 unit tests passed on JDK 21.0.12 with Paper 1.21.11 API.
- The same 47 tests passed on Java 25.0.4.1 with cached Paper 26.3 build 6 alpha API.
- All production source compiled with Java 21 bytecode against each API using Eclipse ECJ.
- Maven's resource, JAR and shade goals package the local artifact, including the MIT license and relocated Gson. GitHub PR CI separately runs the standard `mvn clean verify` build on Java 21 and 25.
- New tests cover lifecycle invalidation and queued scans, retries/rate limits/redirects, malformed source paths and responses, config preservation, checksum validation, unsafe archives, missing metadata/main classes and failed-download cleanup.
- HTTP tests use injected connection fixtures, not live GitHub/Modrinth/Spigot requests. There was no live Paper startup, command or download test in this maintenance run.

Historical build information follows.
