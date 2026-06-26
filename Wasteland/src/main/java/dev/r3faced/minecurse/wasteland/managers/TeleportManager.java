package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages per-skill teleport destinations.
 * <p>
 * Locations are stored in {@code teleports.yml} inside the plugin data
 * folder so they survive restarts and reloads. The format is:
 * <pre>
 * teleports:
 *   mining:
 *     world: "world"
 *     x: 1.5
 *     y: 64.0
 *     z: 1.5
 *     yaw: 0.0
 *     pitch: 0.0
 *   woodcutting:
 *     ...
 * </pre>
 */
public class TeleportManager {

    private final WastelandPlugin plugin;
    private final java.io.File file;
    private FileConfiguration config;

    public TeleportManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        this.file = new java.io.File(plugin.getDataFolder(), "teleports.yml");
        reload();
    }

    /** (Re)load teleports.yml from disk, creating it from the bundled default if missing. */
    public void reload() {
        if (!file.exists()) {
            try {
                plugin.saveResource("teleports.yml", false);
            } catch (Exception ignored) {
                // Bundled default may not exist yet; just create an empty file.
                try {
                    if (!file.exists() && file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not create teleports.yml: " + e.getMessage());
                }
            }
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    /** Save the in-memory config back to disk. */
    private void save() {
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save teleports.yml: " + e.getMessage());
        }
    }

    /**
     * Set the teleport destination for the given skill to the player's
     * current location. Persisted immediately.
     */
    public void setTeleport(SkillType skill, Location loc) {
        if (skill == null || loc == null || loc.getWorld() == null) return;
        String base = "teleports." + skill.getKey();
        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x",     loc.getX());
        config.set(base + ".y",     loc.getY());
        config.set(base + ".z",     loc.getZ());
        config.set(base + ".yaw",   loc.getYaw());
        config.set(base + ".pitch", loc.getPitch());
        save();
    }

    /** Returns true if a teleport destination is configured for this skill. */
    public boolean hasTeleport(SkillType skill) {
        if (skill == null) return false;
        return config.isConfigurationSection("teleports." + skill.getKey());
    }

    /**
     * Get the configured teleport destination for the given skill, or null
     * if none is set or the world is no longer loaded.
     */
    public Location getTeleport(SkillType skill) {
        if (skill == null) return null;
        String base = "teleports." + skill.getKey();
        if (!config.isConfigurationSection(base)) return null;

        String worldName = config.getString(base + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x     = config.getDouble(base + ".x", 0.5);
        double y     = config.getDouble(base + ".y", 64.0);
        double z     = config.getDouble(base + ".z", 0.5);
        float  yaw   = (float) config.getDouble(base + ".yaw", 0.0);
        float  pitch = (float) config.getDouble(base + ".pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
