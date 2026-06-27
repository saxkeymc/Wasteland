package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task that ticks every 10 seconds to accumulate Wasteland
 * playtime for every online player who is currently inside a configured
 * Wasteland world.
 * <p>
 * Playtime is stored on the {@link PlayerData} record and persisted via
 * the data manager. A short tick interval keeps potential data loss on
 * server crash small (max ~10 seconds of unsaved playtime).
 * <p>
 * Additional save points:
 *   • PlayerChangedWorldEvent — flushes the partial session since the
 *     last tick when a player leaves a Wasteland world.
 *   • PlayerQuitEvent — same flush on logout.
 *   • onDisable() — saves every online player synchronously.
 * <p>
 * Players outside Wasteland worlds (hub, spawn, survival, etc.) do NOT
 * accumulate playtime. The check is delegated to
 * {@link WastelandWorldManager#isWastelandWorld(org.bukkit.World)}.
 */
public class PlaytimeTask extends BukkitRunnable {

    /** Tick interval in server ticks (200 = 10 seconds). */
    public static final long INTERVAL_TICKS = 200L;
    private static final long INTERVAL_SECONDS = INTERVAL_TICKS / 20L;

    private final WastelandPlugin plugin;

    public PlaytimeTask(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getDataManager() == null) return;
        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data == null) continue;

            boolean inWl = wwm.isWastelandWorld(player.getWorld());

            if (inWl) {
                // Either this is the first tick the player is in a WL world,
                // or a continuation. Add INTERVAL_SECONDS and stamp the
                // current time so the next tick measures from here.
                if (data.getInWastelandSince() == 0L) {
                    data.setInWastelandSince(now);
                } else {
                    long elapsed = (now - data.getInWastelandSince()) / 1000L;
                    if (elapsed > 0) {
                        data.addPlaytimeSeconds(elapsed);
                    }
                    data.setInWastelandSince(now);
                }
                // Persist async (or sync on shutdown — handled by data manager).
                plugin.getDataManager().savePlayer(player.getUniqueId());
            } else {
                // Player is NOT in a Wasteland world. Clear any stale
                // timestamp (already flushed by WorldChangeListener in
                // normal flow, but this is a safety net).
                if (data.getInWastelandSince() != 0L) {
                    data.setInWastelandSince(0L);
                }
                // Do NOT add playtime. Do NOT save (nothing changed).
            }
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
