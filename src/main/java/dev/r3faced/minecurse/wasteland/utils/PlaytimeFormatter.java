package dev.r3faced.minecurse.wasteland.utils;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public final class PlaytimeFormatter {

    private PlaytimeFormatter() {}

    public static String format(WastelandPlugin plugin, long seconds) {
        if (seconds < 0) seconds = 0;
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        String template = cfg.getString("playtime.format", "{hours}h {minutes}m");
        boolean stripEmpty = cfg.getBoolean("playtime.strip-empty-parts", true);

        long days    = seconds / 86400L;
        long hours   = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs    = seconds % 60L;
        long totalHours = seconds / 3600L;

        String out = template
                .replace("{days}",         String.valueOf(days))
                .replace("{hours}",        String.valueOf(hours))
                .replace("{minutes}",      String.valueOf(minutes))
                .replace("{seconds}",      String.valueOf(secs))
                .replace("{total_hours}",  String.valueOf(totalHours));

        if (stripEmpty) {
            out = out.replaceAll("(?<=^|\\s)0d\\s*", "")
                     .replaceAll("(?<=^|\\s)0h\\s*", "")
                     .replaceAll("(?<=^|\\s)0m\\s*$", "")
                     .replaceAll("\\s{2,}", " ")
                     .trim();
            if (out.isEmpty()) out = "0m";
        }
        return out;
    }
}
