package dev.updatewatch;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MetricsControllerTest {
    @Test void defaultsToEnabledForExistingConfigurations() {
        var controller = new MetricsController(() -> null, Logger.getLogger("test"));
        assertTrue(controller.enabled(new YamlConfiguration()));
    }

    @Test void acceptsExplicitOptOut() {
        var config = new YamlConfiguration();
        config.set("metrics.enabled", false);
        var controller = new MetricsController(() -> null, Logger.getLogger("test"));
        assertFalse(controller.enabled(config));
    }

    @Test void gracefullyHandlesMalformedOptOutByDefaultingToEnabled() {
        for (Object value : new Object[] {"false", "no", 0, 1, 1.5}) {
            var config = new YamlConfiguration();
            config.set("metrics.enabled", value);
            var controller = new MetricsController(() -> null, Logger.getLogger("test"));
            // Should not throw; should log warning and default to true
            assertTrue(controller.enabled(config));
        }
    }

    @Test void disabledDoesNotCreateClient() {
        var controller = new MetricsController(() -> { fail("Metrics must not start"); return () -> {}; });
        controller.configure(false);
        controller.close();
    }

    @Test void reloadDoesNotDuplicateClientsAndOptOutStopsClient() {
        var starts = new AtomicInteger();
        var stops = new AtomicInteger();
        var controller = new MetricsController(() -> { starts.incrementAndGet(); return stops::incrementAndGet; });
        controller.configure(true);
        controller.configure(true);
        assertEquals(1, starts.get());
        controller.configure(false);
        controller.configure(false);
        assertEquals(1, stops.get());
        controller.configure(true);
        assertEquals(2, starts.get());
        controller.close();
        controller.close();
        assertEquals(2, stops.get());
    }

    @Test void failedInitializationCanBeRetried() {
        var attempts = new AtomicInteger();
        var stops = new AtomicInteger();
        var controller = new MetricsController(() -> {
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("Unavailable");
            return stops::incrementAndGet;
        });
        assertThrows(IllegalStateException.class, () -> controller.configure(true));
        controller.configure(true);
        controller.close();
        assertEquals(2, attempts.get());
        assertEquals(1, stops.get());
    }
}
