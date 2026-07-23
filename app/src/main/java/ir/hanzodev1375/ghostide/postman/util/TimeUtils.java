package ir.hanzodev1375.ghostide.postman.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String relativeTime(long timestampMillis) {
        long diff = System.currentTimeMillis() - timestampMillis;
        if (diff < 0) diff = 0;
        if (diff < 60_000L) return "just now";
        if (diff < 3_600_000L) return (diff / 60_000L) + "m ago";
        if (diff < 86_400_000L) return (diff / 3_600_000L) + "h ago";
        if (diff < 7L * 86_400_000L) return (diff / 86_400_000L) + "d ago";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        return sdf.format(new Date(timestampMillis));
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public static String formatDuration(long ms) {
        if (ms < 1000) return ms + " ms";
        return String.format(Locale.US, "%.2f s", ms / 1000.0);
    }
}
