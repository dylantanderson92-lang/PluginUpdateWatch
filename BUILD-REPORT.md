# Build verification — 1.2.0

- Compiled all source against Paper 1.21.11 and cached Paper 26.3 build 6 alpha APIs, targeting Java 21 bytecode.
- All 24 automated tests passed: 10 discovery/config/link tests, 7 Modrinth tests, 5 JAR validation tests and 2 version-comparison tests.
- Discovery tests cover hash matching, saved configuration round trips, unresolved sources, legacy/disabled settings, duplicate JARs, metadata fallback, network errors, duplicate config entries, filename/link validation and checks without discovery.
- Compiled locally with Eclipse ECJ 3.41.0; tests and shading run with Maven. Gson is relocated.
- The new discovery flow has not been run inside a live Paper server. API compilation and fixture-based tests do not establish full runtime compatibility.
- The GitHub release workflow independently builds/tests with Maven on Java 21 and 25 before attaching the distributed JAR. Workflow status is the source of truth for those remote checks.