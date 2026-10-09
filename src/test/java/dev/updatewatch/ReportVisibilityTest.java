package dev.updatewatch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportVisibilityTest {
    @Test void onlyDemonstrablyNewerVersionsTriggerUpdateNotifications() {
        for (var status : Versions.Status.values())
            assertEquals(status == Versions.Status.UPDATE, ReportVisibility.confirmedUpdate(new UpdateCheckService.Result(null, null, status, null)));
        assertFalse(ReportVisibility.confirmedUpdate(new UpdateCheckService.Result(null, null, Versions.Status.UPDATE, "Provider failed")));
    }
    @Test void consoleShowsConfirmedUpdatesAndErrorsWhileFullReportsRetainUnconfirmedResults() {
        for (var status : Versions.Status.values()) {
            var result = new UpdateCheckService.Result(null, null, status, null);
            assertEquals(status == Versions.Status.UPDATE, ReportVisibility.result(true, result));
            assertTrue(ReportVisibility.result(false, result));
        }
        assertTrue(ReportVisibility.result(true, new UpdateCheckService.Result(null, null, null, "[ERROR] NETWORK_ERROR")));
    }
    @Test void disabledNotesRemainVisibleToPlayersAndProblemsRemainVisibleInConsole() {
        assertFalse(ReportVisibility.note(true, "[INFO] Disabled in existing configuration"));
        assertTrue(ReportVisibility.note(false, "[INFO] Disabled in existing configuration"));
        assertTrue(ReportVisibility.note(true, "Source not identified; paste a source link"));
        assertTrue(ReportVisibility.note(true, "[ERROR] Invalid config"));
    }
}
