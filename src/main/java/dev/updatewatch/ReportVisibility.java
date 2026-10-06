package dev.updatewatch;

/** Console reports contain actionable results; player reports retain the full inventory. */
final class ReportVisibility {
    static boolean result(boolean console, UpdateCheckService.Result result) {
        return !console || result.error() != null || result.status() != Versions.Status.CURRENT;
    }
    static boolean note(boolean console, String note) { return !console || !note.startsWith("[INFO]"); }
}
