package dev.updatewatch;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VersionsTest {
    @Test void comparesNumericSegments() {
        assertEquals(Versions.Status.UPDATE, Versions.compare("1.9", "1.10"));
        assertEquals(Versions.Status.CURRENT, Versions.compare("2.0", "1.99"));
        assertEquals(Versions.Status.CURRENT, Versions.compare("v1.2.0+build7", "1.2"));
        assertEquals(Versions.Status.UPDATE, Versions.compare("1", "1.0.1"));
    }
    @Test void customVersionsAreNotClaimedNewer() {
        assertEquals(Versions.Status.DIFFERENT, Versions.compare("1.2-dev", "1.2"));
        assertEquals(Versions.Status.DIFFERENT, Versions.compare("build-78", "build-79"));
        assertEquals(Versions.Status.CURRENT, Versions.compare("v1.2-RC1", "1.2-rc1"));
    }
    @Test void planBuildFormatsCompareTheActualBuildNumber() {
        for (String installed : java.util.List.of("5.8 build 3638", "5.8+build.3638", "5.8-build-3638", "5.8.3638"))
            for (String offered : java.util.List.of("5.8 build 3638", "5.8+build.3638", "5.8.3638"))
                assertEquals(Versions.Status.CURRENT, Versions.compare(installed, offered));
        assertEquals(Versions.Status.UPDATE, Versions.compare("5.8 build 3605", "5.8+build.3638"));
        assertEquals(Versions.Status.CURRENT, Versions.compare("5.8 build 3638", "5.8+build.3605"));
    }
    @Test void platformLabelsDoNotInventUpdates() {
        for (String offered : java.util.List.of("paper-1.4.9", "v1.4.9-spigot", "1.4.9-bukkit", "Paper-v1.4.9"))
            assertEquals(Versions.Status.CURRENT, Versions.compare("1.4.9", offered));
        assertEquals(Versions.Status.UPDATE, Versions.compare("paper-1.4.9", "paper-1.4.10"));
        assertEquals(Versions.Status.DIFFERENT, Versions.compare("1.4.9", "fabric-1.4.9"));
    }
    @Test void differentDevelopmentBuildIdentifiersRemainUncertain() {
        assertEquals(Versions.Status.DIFFERENT, Versions.compare("2.22.1-dev+25-cfb6f12", "2.22.1-dev+27-e70bdb8"));
        assertEquals(Versions.Status.CURRENT, Versions.compare("2.22.1-dev+27-e70bdb8", "2.22.1-dev+27-e70bdb8"));
    }
    @Test void reportedDylyCraftExamplesSeparateFormattingFromGenuineUpdates() {
        assertEquals(Versions.Status.CURRENT, Versions.compare("5.8 build 3638", "5.8+build.3638")); // Plan
        assertEquals(Versions.Status.CURRENT, Versions.compare("5.5.71", "v5.5.71-bukkit")); // LuckPerms
        assertEquals(Versions.Status.CURRENT, Versions.compare("1.4.9", "paper-1.4.9")); // DoubleDoors
        assertEquals(Versions.Status.UPDATE, Versions.compare("0.1.8.71", "0.1.8.72")); // BigDoors
        assertEquals(Versions.Status.UPDATE, Versions.compare("2.4.5", "2.4.6")); // ResourcePackManager
    }
}
