package dev.updatewatch;

/** Server-thread-owned lifecycle gate. Reload invalidates results without starting overlapping work. */
final class CheckState {
    private long generation;
    private boolean running;
    private boolean pending;
    boolean running() { return running; }
    long begin() {
        if (running) throw new IllegalStateException("Check already running");
        running = true; return generation;
    }
    boolean finish(long epoch) { running = false; return epoch == generation; }
    void invalidate() { generation++; if (running) pending = true; }
    void requestScan() { pending = true; }
    boolean takePendingScan() {
        if (running || !pending) return false;
        pending = false; return true;
    }
}
