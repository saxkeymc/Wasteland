package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Determines whether a given world counts as a "Wasteland world" for
 * playtime-tracking purposes.
 * <p>
 * The list is configurable in config.yml under {@code wasteland-worlds}.
 * If the list is empty or missing, the plugin falls back to all worlds
 * configured under the {@code worlds} section (one per skill).
 */
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

        // Primary source: explicit wasteland-worlds list
        for (String name : cfg.getStringList("wasteland-worlds")) {
            if (name != null && !name.trim().isEmpty()) {
                wastelandWorlds.add(name.trim());
            }
        }

        // Fallback: derive from the per-skill worlds section
        if (wastelandWorlds.isEmpty()) {
            for (SkillTypeHelper skill : SkillTypeHelper.values()) {
                String worldName = cfg.getString("worlds." + skill.key + ".world");
                if (worldName != null && !worldName.trim().isEmpty()) {
                    wastelandWorlds.add(worldName.trim());
                }
            }
        }
    }

    /** Returns true if the given world name is configured as a Wasteland world. */
    public boolean isWastelandWorld(String worldName) {
        if (worldName == null) return false;
        return wastelandWorlds.contains(worldName);
    }

    /** Returns true if the given world is configured as a Wasteland world. */
    public boolean isWastelandWorld(World world) {
        return world != null && isWastelandWorld(world.getName());
    }

    /** Returns the set of all configured Wasteland world names. */
    public Set<String> getWastelandWorlds() {
        return new HashSet<>(wastelandWorlds);
    }

    /** Lightweight enum mirror so we don't import SkillType just for its key. */
    private enum SkillTypeHelper {
        MINING("mining"),
        WOODCUTTING("woodcutting"),
        FARMING("farming"),
        FISHING("fishing");

        final String key;
        SkillTypeHelper(String key) { this.key = key; }
    }
}
