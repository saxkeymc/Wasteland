package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

public class WastelandWorldManager {

    private final WastelandPlugin plugin;
    private Set<String> wastelandWorlds = new HashSet<>();

    public WastelandWorldManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        wastelandWorlds = new HashSet<>();

        for (String name : cfg.getStringList("wasteland-worlds")) {
            if (name != null && !name.trim().isEmpty()) {
                wastelandWorlds.add(name.trim());
            }
        }

        if (wastelandWorlds.isEmpty()) {
            for (SkillTypeHelper skill : SkillTypeHelper.values()) {
                String worldName = cfg.getString("worlds." + skill.key + ".world");
                if (worldName != null && !worldName.trim().isEmpty()) {
                    wastelandWorlds.add(worldName.trim());
                }
            }
        }
    }

    public boolean isWastelandWorld(String worldName) {
        if (worldName == null) return false;
        return wastelandWorlds.contains(worldName);
    }

    public boolean isWastelandWorld(World world) {
        return world != null && isWastelandWorld(world.getName());
    }

    public Set<String> getWastelandWorlds() {
        return new HashSet<>(wastelandWorlds);
    }

    private enum SkillTypeHelper {
        MINING("mining"),
        WOODCUTTING("woodcutting"),
        FARMING("farming"),
        FISHING("fishing");

        final String key;
        SkillTypeHelper(String key) { this.key = key; }
    }
}
