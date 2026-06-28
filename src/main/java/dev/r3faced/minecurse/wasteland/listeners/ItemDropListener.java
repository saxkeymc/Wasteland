package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Prevents players from dropping items in Wasteland worlds.
 * All drops are cancelled silently.
 */
public class ItemDropListener implements Listener {

    private final WastelandPlugin plugin;

    public ItemDropListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        event.setCancelled(true);
    }
}
