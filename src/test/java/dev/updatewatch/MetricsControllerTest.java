package dev.updatewatch;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class MetricsControllerTest {
    @Test void defaultsToEnabledForExistingConfigurations() {
        assertTrue(MetricsController.enabled(new YamlConfiguration()));
    }

    @Test void acceptsExplicitOptOut() {
        var config = new YamlConfiguration();
        config.set("metrics.enabled", false);
        assertFalse(MetricsController.enabled(config));
    }

    @Test void rejectsMalformedOptOutInsteadOfEnablingMetrics() {
        for (Object value : new Object[] {"false", "no", 0, 1}) {
            var config = new YamlConfiguration();
            config.set("metrics.enabled", value);
            assertThrows(IllegalArgumentException.class, () -> MetricsController.enabled(config));
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
