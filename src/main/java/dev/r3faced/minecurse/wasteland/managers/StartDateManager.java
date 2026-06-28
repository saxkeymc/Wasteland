package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages the day-based tier progression system.
 * <p>
 * When an admin runs /wasteland start, the current date is recorded.
 * Each tier becomes available on consecutive days:
 * <ul>
 *   <li>Day 0 (start day): Tier 1 available</li>
 *   <li>Day 1: Tier 2 available</li>
 *   <li>Day 2: Tier 3 available</li>
 *   <li>Day 3: Tier 4 available</li>
 *   <li>Day 4: Tier 5 available</li>
 * </ul>
 * Players cannot unlock a tier until its day has arrived, even if they
 * meet the level requirements.
 */
public class StartDateManager {

    private final WastelandPlugin plugin;
    private long startTimestamp = 0L; // epoch-millis when /wasteland start was run
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

    /**
     * Record the start date. Called when /wasteland start is run.
     */
    public void start() {
        startTimestamp = System.currentTimeMillis();
        started = true;

        FileConfiguration cfg = plugin.getConfig();
        cfg.set("start-date.timestamp", startTimestamp);
        plugin.saveConfig();
    }

    /**
     * Returns true if /wasteland start has been run.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Returns the number of days since /wasteland start was run.
     * Returns -1 if not started.
     */
    public int getDaysSinceStart() {
        if (!started) return -1;
        long elapsed = System.currentTimeMillis() - startTimestamp;
        return (int) (elapsed / (1000L * 60 * 60 * 24));
    }

    /**
     * Returns the maximum tier that is currently available based on
     * days since start. Tier 1 is available on day 0, Tier 2 on day 1, etc.
     * Returns 0 if not started.
     */
    public int getMaxAvailableTier() {
        if (!started) return 0;
        int days = getDaysSinceStart();
        // Tier 1 = day 0, Tier 2 = day 1, Tier 3 = day 2, etc.
        return Math.min(days + 1, TierManager.TIER_COUNT);
    }

    /**
     * Returns true if the given tier is currently available (its day has arrived).
     */
    public boolean isTierAvailable(int tier) {
        if (!started) return tier <= 1; // Tier 1 always available
        return tier <= getMaxAvailableTier();
    }

    /**
     * Returns a human-readable start date string.
     */
    public String getStartDateString() {
        if (!started) return "Not started";
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(startTimestamp));
    }
}
