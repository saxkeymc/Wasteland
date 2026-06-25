package dev.r3faced.minecurse.wasteland.data.yaml;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.data.DataManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores player data in individual YAML files under plugins/Wasteland/data/playerdata/.
 * <p>
 * Saves are dispatched asynchronously via Bukkit's scheduler when the plugin
 * is enabled. When the plugin is disabling (onDisable / shutdown), saves are
 * performed synchronously on the calling thread to avoid
 * {@code IllegalPluginAccessException}.
 */
public class YamlDataManager implements DataManager {

    private final WastelandPlugin plugin;
    private final File playerDataDir;

    /** In-memory cache of loaded player data. */
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public YamlDataManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        this.playerDataDir = new File(plugin.getDataFolder(), "data/playerdata");
    }

    @Override
    public void init() {
        if (!playerDataDir.exists() && !playerDataDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create playerdata directory!");
        }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        PlayerData data = loadFromDisk(uuid);
        cache.put(uuid, data);
        return data;
    }

    @Override
    public void savePlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        if (!plugin.isEnabled()) {
            saveToDisk(data);
            return;
        }

        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveToDisk(data);
                }
            }.runTaskAsynchronously(plugin);
        } catch (RuntimeException ex) {
            saveToDisk(data);
        }
    }

    @Override
    public void savePlayerSync(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        saveToDisk(data);
    }

    @Override
    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveToDisk(data);
        }
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    @Override
    public void resetPlayer(UUID uuid) {
        PlayerData fresh = new PlayerData(uuid);
        cache.put(uuid, fresh);
        if (plugin.isEnabled()) {
            try {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        saveToDisk(fresh);
                    }
                }.runTaskAsynchronously(plugin);
            } catch (RuntimeException ex) {
                saveToDisk(fresh);
            }
        } else {
            saveToDisk(fresh);
        }
    }

    @Override
    public void shutdown() {
        saveAll();
        cache.clear();
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private File getFile(UUID uuid) {
        return new File(playerDataDir, uuid.toString() + ".yml");
    }

    private PlayerData loadFromDisk(UUID uuid) {
        File file = getFile(uuid);
        PlayerData data = new PlayerData(uuid);

        if (!file.exists()) {
            return data;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();
            data.setLevel(skill, yaml.getInt("skills." + key + ".level", 0));
            data.setXp(skill, yaml.getLong("skills." + key + ".xp", 0L));
        }

        // Single shared tier
        data.setTier(yaml.getInt("tier", 1));

        // Claimed tier rewards (key format: "tier_N")
        Set<String> claimed = new HashSet<>(yaml.getStringList("claimed-tiers"));
        data.setClaimedTiers(claimed);

        // Playtime (seconds)
        data.setPlaytimeSeconds(yaml.getLong("playtime-seconds", 0L));

        return data;
    }

    private void saveToDisk(PlayerData data) {
        File file = getFile(data.getUuid());
        YamlConfiguration yaml = new YamlConfiguration();

        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();
            yaml.set("skills." + key + ".level", data.getLevel(skill));
            yaml.set("skills." + key + ".xp",    data.getXp(skill));
        }

        yaml.set("tier", data.getTier());
        yaml.set("claimed-tiers", Arrays.asList(data.getClaimedTiers().toArray(new String[0])));
        yaml.set("playtime-seconds", data.getPlaytimeSeconds());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}
