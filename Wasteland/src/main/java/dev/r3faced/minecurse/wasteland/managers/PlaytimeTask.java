package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task that ticks every minute to accumulate Wasteland playtime
 * for every online player.
 * <p>
 * Playtime is stored on the {@link PlayerData} record and persisted via
 * the data manager. A short tick interval keeps potential data loss on
 * server crash small (max ~1 minute of unsaved playtime).
 */
public class PlaytimeTask extends BukkitRunnable {

    /** Tick interval in server ticks (1200 = 60 seconds). */
    public static final long INTERVAL_TICKS = 1200L;
    private static final long INTERVAL_SECONDS = INTERVAL_TICKS / 20L;

    private final WastelandPlugin plugin;

    public PlaytimeTask(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getDataManager() == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data == null) continue;
            data.addPlaytimeSeconds(INTERVAL_SECONDS);
            // Persist async (or sync on shutdown — handled by data manager).
            plugin.getDataManager().savePlayer(player.getUniqueId());
        }
    }

    /** Start the periodic task. Safe to call once during onEnable. */
    public void start() {
        try {
            runTaskTimer(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not schedule playtime task: " + ex.getMessage());
        }
    }
}
