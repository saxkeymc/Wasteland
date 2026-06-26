package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Detects players entering Wasteland worlds (on join or world change) and
 * ensures they hold the correct Omni Tool. Also loads player data on join,
 * saves/unloads on quit, and flushes any unsaved Wasteland playtime
 * accumulated since the last periodic tick.
 * <p>
 * Playtime accounting:
 *   • Join:        if joining directly into a WL world, stamp
 *                  {@code inWastelandSince = now}.
 *   • WorldChange: if leaving a WL world, flush elapsed playtime and
 *                  clear the timestamp. If entering a WL world, stamp
 *                  the timestamp.
 *   • Quit:        if currently in a WL world, flush elapsed playtime
 *                  and clear the timestamp.
 *   • Periodic task (PlaytimeTask) handles the ongoing accumulation.
 */
public class WorldChangeListener implements Listener {

    private final WastelandPlugin plugin;

    public WorldChangeListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load data into cache
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);

        // If joining directly into a Wasteland world, start the playtime clock.
        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        if (wwm.isWastelandWorld(player.getWorld())) {
            data.setInWastelandSince(System.currentTimeMillis());
        } else {
            data.setInWastelandSince(0L);
        }

        // Give tool if already in a wasteland world
        checkAndGiveTool(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        boolean wasInWl = wwm.isWastelandWorld(event.getFrom());
        boolean nowInWl = wwm.isWastelandWorld(player.getWorld());
        long now = System.currentTimeMillis();

        if (wasInWl && !nowInWl) {
            // Leaving a Wasteland world — flush elapsed playtime since the
            // last tick and stop the clock.
            flushSession(data, now);
            plugin.getDataManager().savePlayer(player.getUniqueId());
        } else if (!wasInWl && nowInWl) {
            // Entering a Wasteland world — start the clock.
            data.setInWastelandSince(now);
        }
        // If both were WL worlds, the timestamp stays as-is (continuous session).

        // Re-equip the appropriate Omni Tool for the new world.
        checkAndGiveTool(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        if (data != null) {
            // Flush any unflushed playtime before saving.
            flushSession(data, System.currentTimeMillis());
        }

        // Save player data, then unload from cache.
        plugin.getDataManager().savePlayer(uuid);
        plugin.getDataManager().unloadPlayer(uuid);
    }

    /**
     * If the player's inWastelandSince is non-zero, compute the elapsed
     * time since that timestamp and add it to the player's playtime,
     * then reset the timestamp to 0.
     */
    private void flushSession(PlayerData data, long now) {
        long since = data.getInWastelandSince();
        if (since > 0L) {
            long elapsed = (now - since) / 1000L;
            if (elapsed > 0) {
                data.addPlaytimeSeconds(elapsed);
            }
            data.setInWastelandSince(0L);
        }
    }

    private void checkAndGiveTool(Player player) {
        String worldName = player.getWorld().getName();
        SkillType skill = plugin.getToolManager().getSkillForWorld(worldName);
        if (skill != null) {
            plugin.getToolManager().giveOmniTool(player, skill);
        }
    }
}
