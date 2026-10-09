package dev.updatewatch;

import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.configuration.file.YamlConfiguration;

/** Server-thread lifecycle for the optional bStats client. */
final class MetricsController implements AutoCloseable {
    private final Supplier<Runnable> start;
    private Runnable shutdown;

    MetricsController(Supplier<Runnable> start) { this.start = start; }

    static boolean enabled(YamlConfiguration config) {
        if (!config.contains("metrics.enabled")) return true;
        Object value = config.get("metrics.enabled");
        if (!(value instanceof Boolean enabled)) throw new IllegalArgumentException("metrics.enabled must be true or false");
        return enabled;
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
