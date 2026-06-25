package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players entering Wasteland worlds (on join or world change) and
 * ensures they hold the correct Omni Tool. Also loads player data on join,
 * saves/unloads on quit, and flushes any unsaved Wasteland playtime on quit.
 */
public class WorldChangeListener implements Listener {

    private final WastelandPlugin plugin;

    /** Records the epoch-millis join time for each online player. */
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public WorldChangeListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load data into cache
        plugin.getDataManager().getPlayerData(player.getUniqueId());
        // Track join time for playtime accounting
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());
        // Give tool if already in a wasteland world
        checkAndGiveTool(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkAndGiveTool(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Flush any playtime accumulated since the last periodic save.
        Long joinedAt = joinTimes.remove(uuid);
        if (joinedAt != null) {
            long sessionSeconds = (System.currentTimeMillis() - joinedAt) / 1000L;
            if (sessionSeconds > 0) {
                PlayerData data = plugin.getDataManager().getPlayerData(uuid);
                if (data != null) {
                    data.addPlaytimeSeconds(sessionSeconds);
                }
            }
        }

        // Save player data, then unload from cache.
        plugin.getDataManager().savePlayer(uuid);
        plugin.getDataManager().unloadPlayer(uuid);
    }

    private void checkAndGiveTool(Player player) {
        String worldName = player.getWorld().getName();
        SkillType skill = plugin.getToolManager().getSkillForWorld(worldName);
        if (skill != null) {
            plugin.getToolManager().giveOmniTool(player, skill);
        }
    }
}
