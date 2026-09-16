# Build verification

- Compiled against Paper API `1.21.11-R0.1-SNAPSHOT` with Java 21 bytecode.
- All 7 JUnit tests passed: numeric/custom version handling; matching Bukkit and Paper descriptors; rejection of mismatched plugin names, non-plugin JARs, and HTML responses.
- Packaged with Maven Shade; bundled Gson is relocated into `dev.updatewatch.lib.gson`.
- The local Windows sandbox caused javac's ZIP filesystem cleanup to fail with an access-denied error. This build used Eclipse ECJ 3.41.0 to compile and Maven Surefire/JAR/Shade to test and package. The supplied Maven project uses standard javac for normal environments.
- No live Paper server integration test was performed. Paper 1.21.11–26.3 is the intended compatibility range; runtime compatibility across that range is not verified.
- External API lookups, rate limits, and real publisher downloads were not exercised by the automated tests.
