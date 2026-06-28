package dev.r3faced.minecurse.wasteland.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which blocks are currently showing as fake bedrock for each player.
 * When a player interacts with a fake bedrock block, the server sends a
 * block-update packet that restores the original visual — this manager
 * allows us to re-send the bedrock visual to prevent that.
 */
public class FakeBlockManager {

    /** Map: player UUID → set of locations currently showing as bedrock. */
    private final Map<UUID, Map<Location, Material>> fakeBlocks = new ConcurrentHashMap<>();

    /**
     * Record that a block is showing as fake bedrock for a player.
     */
    public void addFakeBlock(Player player, Location loc, Material original) {
        fakeBlocks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(loc, original);
    }

    /**
     * Remove a fake block record (after the 6-second regen).
     */
    public void removeFakeBlock(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        if (map != null) {
            map.remove(loc);
        }
    }

    /**
     * Check if a location is currently fake bedrock for a player.
     */
    public boolean isFakeBedrock(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        return map != null && map.containsKey(loc);
    }

    /**
     * Get the original material for a fake bedrock location.
     */
    public Material getOriginalMaterial(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        return map != null ? map.get(loc) : null;
    }

    /**
     * Clean up all fake blocks for a player (on quit / world change).
     */
    public void clearPlayer(UUID uuid) {
        fakeBlocks.remove(uuid);
    }
}
