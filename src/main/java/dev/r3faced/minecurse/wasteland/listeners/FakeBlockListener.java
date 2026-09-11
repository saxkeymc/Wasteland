package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

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
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendBlockChange(loc, Material.BEDROCK, (byte) 0);
            }, 1L);
        }
    }
}
