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

/**
 * Teleports players back up if they fall below Y=90 in a Wasteland world.
 * Records the player's last "safe" position (above Y=90) and teleports
 * them there if they fall below.
 */
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

        // Record safe position when above Y=90.
        if (loc.getY() >= 90) {
            lastSafePos.put(player.getUniqueId(), loc.clone());
        }

        // Teleport back if below Y=90.
        if (loc.getY() < 90) {
            Location safe = lastSafePos.get(player.getUniqueId());
            if (safe != null) {
                player.teleport(safe);
                player.setFallDistance(0);
            } else {
                // No safe position recorded — teleport to spawn.
                player.teleport(player.getWorld().getSpawnLocation());
                player.setFallDistance(0);
            }
        }
    }
}
