package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Re-sends the fake bedrock visual when a player interacts with a
 * fake bedrock block. On Spigot 1.8.8, clicking a block sends a
 * block-update packet that restores the original visual — this
 * listener intercepts that and re-sends the bedrock.
 */
public class FakeBlockListener implements Listener {

    private final WastelandPlugin plugin;

    public FakeBlockListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        org.bukkit.Location loc = event.getClickedBlock().getLocation();

        if (plugin.getFakeBlockManager().isFakeBedrock(player, loc)) {
            // Re-send the bedrock visual 1 tick later (after the server's
            // block-update packet from the interaction).
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendBlockChange(loc, Material.BEDROCK, (byte) 0);
            }, 1L);
        }
    }
}
