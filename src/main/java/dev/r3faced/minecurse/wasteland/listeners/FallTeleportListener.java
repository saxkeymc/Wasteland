package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FallTeleportListener implements Listener {

    private final WastelandPlugin plugin;
    private final Map<UUID, Location> lastSafePos = new HashMap<>();

    public FallTeleportListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        Location loc = player.getLocation();

        if (loc.getY() >= 90) {
            lastSafePos.put(player.getUniqueId(), loc.clone());
        }

        if (loc.getY() < 90) {
            Location safe = lastSafePos.get(player.getUniqueId());
            if (safe != null) {
                player.teleport(safe);
                player.setFallDistance(0);
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
                player.setFallDistance(0);
            }
        }
    }
}
