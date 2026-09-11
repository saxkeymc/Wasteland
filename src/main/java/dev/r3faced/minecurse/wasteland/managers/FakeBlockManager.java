package dev.r3faced.minecurse.wasteland.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FakeBlockManager {

    private final Map<UUID, Map<Location, Material>> fakeBlocks = new ConcurrentHashMap<>();

    public void addFakeBlock(Player player, Location loc, Material original) {
        fakeBlocks.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(loc, original);
    }

    public void removeFakeBlock(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        if (map != null) {
            map.remove(loc);
        }
    }

    public boolean isFakeBedrock(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        return map != null && map.containsKey(loc);
    }

    public Material getOriginalMaterial(Player player, Location loc) {
        Map<Location, Material> map = fakeBlocks.get(player.getUniqueId());
        return map != null ? map.get(loc) : null;
    }

    public void clearPlayer(UUID uuid) {
        fakeBlocks.remove(uuid);
    }
}
