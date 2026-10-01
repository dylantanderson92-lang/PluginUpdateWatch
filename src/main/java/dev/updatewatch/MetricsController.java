package dev.updatewatch;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/** Server-thread lifecycle for the optional bStats client. */
final class MetricsController implements AutoCloseable {
    private final Supplier<Runnable> start;
    private final Logger logger;
    private Runnable shutdown;

    MetricsController(Supplier<Runnable> start) {
        this(start, Logger.getLogger("PluginUpdateWatch"));
    }

    MetricsController(Supplier<Runnable> start, Logger logger) {
        this.start = start;
        this.logger = logger;
    }

    /**
     * Check if metrics should be enabled based on config.
     * Malformed values log a warning and default to true (enabled).
     * This ensures a config typo does not disable the entire plugin.
     */
    boolean enabled(YamlConfiguration config) {
        if (!config.contains("metrics.enabled")) return true;
        Object value = config.get("metrics.enabled");
        if (value instanceof Boolean enabled) return enabled;
        logger.warning("metrics.enabled must be true or false; found " + 
            (value == null ? "null" : value.getClass().getSimpleName()) + 
            ". Defaulting to enabled.");
        return true;
    }

    void configure(boolean enabled) {
        if (!enabled) close();
        else if (shutdown == null) shutdown = Objects.requireNonNull(start.get());
    }

    @Override public void close() {
        if (shutdown == null) return;
        Runnable stop = shutdown;
        shutdown = null;
        stop.run();
    }
}
