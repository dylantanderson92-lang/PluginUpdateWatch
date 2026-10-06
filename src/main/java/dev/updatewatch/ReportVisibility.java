package dev.updatewatch;

/** Console reports contain actionable results; player reports retain the full inventory. */
final class ReportVisibility {
    static boolean confirmedUpdate(UpdateCheckService.Result result) {
        return result.error() == null && result.status() == Versions.Status.UPDATE;
    }
    static boolean result(boolean console, UpdateCheckService.Result result) {
        return !console || result.error() != null || result.status() != Versions.Status.CURRENT;
    }
    static boolean note(boolean console, String note) { return !console || !note.startsWith("[INFO]"); }
}
