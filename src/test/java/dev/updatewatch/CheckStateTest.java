package dev.updatewatch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckStateTest {
    @Test void reloadDiscardsOldCompletionAndQueuesExactlyOneFreshScan() {
        var state = new CheckState(); long old = state.begin();
        state.invalidate(); state.invalidate();
        assertTrue(state.running()); assertFalse(state.takePendingScan());
        assertThrows(IllegalStateException.class, state::begin);
        assertFalse(state.finish(old)); assertTrue(state.takePendingScan()); assertFalse(state.takePendingScan());
        long next = state.begin(); assertTrue(state.finish(next));
    }
    @Test void scheduledScanWaitsForRunningCheck() {
        var state = new CheckState(); long check = state.begin(); state.requestScan();
        assertFalse(state.takePendingScan()); assertTrue(state.finish(check)); assertTrue(state.takePendingScan());
    }
}
