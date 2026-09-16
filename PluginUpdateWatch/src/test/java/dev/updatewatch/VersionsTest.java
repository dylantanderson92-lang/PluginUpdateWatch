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
}
