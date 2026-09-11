package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StartDateManager {

    private final WastelandPlugin plugin;
    private long startTimestamp = 0L;
    private boolean started = false;

    public StartDateManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        startTimestamp = cfg.getLong("start-date.timestamp", 0L);
        started = startTimestamp > 0L;
    }

    public void start() {
        startTimestamp = System.currentTimeMillis();
        started = true;

        FileConfiguration cfg = plugin.getConfig();
        cfg.set("start-date.timestamp", startTimestamp);
        plugin.saveConfig();
    }

    public boolean isStarted() {
        return started;
    }

    public int getDaysSinceStart() {
        if (!started) return -1;
        long elapsed = System.currentTimeMillis() - startTimestamp;
        return (int) (elapsed / (1000L * 60 * 60 * 24));
    }

    public int getMaxAvailableTier() {
        if (!started) return 0;
        int days = getDaysSinceStart();
        return Math.min(days + 1, TierManager.TIER_COUNT);
    }

    public boolean isTierAvailable(int tier) {
        if (!started) return true;
        return tier <= getMaxAvailableTier();
    }

    public String getStartDateString() {
        if (!started) return "Not started";
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(startTimestamp));
    }
}
