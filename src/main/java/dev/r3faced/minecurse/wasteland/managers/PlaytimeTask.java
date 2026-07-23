package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlaytimeTask extends BukkitRunnable {

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
                if (data.getInWastelandSince() == 0L) {
                    data.setInWastelandSince(now);
                } else {
                    long elapsed = (now - data.getInWastelandSince()) / 1000L;
                    if (elapsed > 0) {
                        data.addPlaytimeSeconds(elapsed);
                    }
                    data.setInWastelandSince(now);
                }
                plugin.getDataManager().savePlayer(player.getUniqueId());
            } else {
                if (data.getInWastelandSince() != 0L) {
                    data.setInWastelandSince(0L);
                }
            }
        }
    }

    public void start() {
        try {
            runTaskTimer(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not schedule playtime task: " + ex.getMessage());
        }
    }
}
