package dev.r3faced.minecurse.wasteland.data.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.data.DataManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL storage backend for Wasteland player data.
 * <p>
 * Uses HikariCP for connection pooling. Async saves are dispatched via
 * Bukkit's scheduler when the plugin is enabled; during shutdown saves
 * are performed synchronously to avoid IllegalPluginAccessException.
 */
public class MySQLDataManager implements DataManager {

    private final WastelandPlugin plugin;
    private HikariDataSource dataSource;

    /** In-memory cache so repeated reads don't hit the database. */
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public MySQLDataManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://"
                + cfg.getString("storage.mysql.host", "localhost") + ":"
                + cfg.getInt("storage.mysql.port", 3306) + "/"
                + cfg.getString("storage.mysql.database", "wasteland")
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        hikari.setUsername(cfg.getString("storage.mysql.username", "root"));
        hikari.setPassword(cfg.getString("storage.mysql.password", ""));
        hikari.setMaximumPoolSize(cfg.getInt("storage.mysql.pool-size", 10));
        hikari.setConnectionTimeout(cfg.getLong("storage.mysql.connection-timeout", 30000));
        hikari.setIdleTimeout(cfg.getLong("storage.mysql.idle-timeout", 600000));
        hikari.setMaxLifetime(cfg.getLong("storage.mysql.max-lifetime", 1800000));
        hikari.setPoolName("WastelandPool");

        dataSource = new HikariDataSource(hikari);

        createTables();
        plugin.getLogger().info("Connected to MySQL database.");
    }

    private void createTables() {
        // Single shared tier + playtime-seconds; no per-skill tier columns,
        // no total_rewards_claimed column (rewards are random per claim).
        String sql = "CREATE TABLE IF NOT EXISTS wasteland_players ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "mining_level INT NOT NULL DEFAULT 0,"
                + "mining_xp BIGINT NOT NULL DEFAULT 0,"
                + "woodcutting_level INT NOT NULL DEFAULT 0,"
                + "woodcutting_xp BIGINT NOT NULL DEFAULT 0,"
                + "farming_level INT NOT NULL DEFAULT 0,"
                + "farming_xp BIGINT NOT NULL DEFAULT 0,"
                + "fishing_level INT NOT NULL DEFAULT 0,"
                + "fishing_xp BIGINT NOT NULL DEFAULT 0,"
                + "tier INT NOT NULL DEFAULT 1,"
                + "claimed_tiers TEXT,"
                + "playtime_seconds BIGINT NOT NULL DEFAULT 0,"
                + "stored_rewards TEXT,"
                + "settings_see_players BOOLEAN NOT NULL DEFAULT TRUE,"
                + "settings_xp_noises BOOLEAN NOT NULL DEFAULT TRUE,"
                + "settings_xp_bar_display BOOLEAN NOT NULL DEFAULT TRUE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create MySQL tables: " + e.getMessage());
        }

        // Graceful upgrade for existing tables: add new columns if missing.
        String[] alters = {
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS tier INT NOT NULL DEFAULT 1",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS claimed_tiers TEXT",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS playtime_seconds BIGINT NOT NULL DEFAULT 0",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS stored_rewards TEXT",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS settings_see_players BOOLEAN NOT NULL DEFAULT TRUE",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS settings_xp_noises BOOLEAN NOT NULL DEFAULT TRUE",
            "ALTER TABLE wasteland_players ADD COLUMN IF NOT EXISTS settings_xp_bar_display BOOLEAN NOT NULL DEFAULT TRUE"
        };
        for (String alter : alters) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(alter)) {
                stmt.execute();
            } catch (SQLException ignored) {
                // Some MySQL versions don't support ADD COLUMN IF NOT EXISTS; ignore.
            }
        }
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }
        PlayerData data = loadFromDatabase(uuid);
        cache.put(uuid, data);
        return data;
    }

    @Override
    public void savePlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        if (!plugin.isEnabled()) {
            saveToDatabase(data);
            return;
        }

        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    saveToDatabase(data);
                }
            }.runTaskAsynchronously(plugin);
        } catch (RuntimeException ex) {
            saveToDatabase(data);
        }
    }

    @Override
    public void savePlayerSync(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        saveToDatabase(data);
    }

    @Override
    public void saveAll() {
        for (PlayerData data : cache.values()) {
            saveToDatabase(data);
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
                        saveToDatabase(fresh);
                    }
                }.runTaskAsynchronously(plugin);
            } catch (RuntimeException ex) {
                saveToDatabase(fresh);
            }
        } else {
            saveToDatabase(fresh);
        }
    }

    @Override
    public void shutdown() {
        saveAll();
        cache.clear();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private PlayerData loadFromDatabase(UUID uuid) {
        PlayerData data = new PlayerData(uuid);

        String sql = "SELECT * FROM wasteland_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                for (SkillType skill : SkillType.values()) {
                    String key = skill.getKey();
                    data.setLevel(skill,  rs.getInt(key + "_level"));
                    data.setXp(skill,     rs.getLong(key + "_xp"));
                }
                try { data.setTier(rs.getInt("tier")); } catch (SQLException ignored) {}
                String rawClaimed = null;
                try { rawClaimed = rs.getString("claimed_tiers"); } catch (SQLException ignored) {}
                if (rawClaimed != null && !rawClaimed.isEmpty()) {
                    Set<String> claimed = new HashSet<>(Arrays.asList(rawClaimed.split(",")));
                    data.setClaimedTiers(claimed);
                }
                try { data.setPlaytimeSeconds(rs.getLong("playtime_seconds")); } catch (SQLException ignored) {}

                // Player settings
                try { data.setSettingSeePlayers(rs.getBoolean("settings_see_players")); } catch (SQLException ignored) {}
                try { data.setSettingXpNoises(rs.getBoolean("settings_xp_noises")); } catch (SQLException ignored) {}
                try { data.setSettingXpBarDisplay(rs.getBoolean("settings_xp_bar_display")); } catch (SQLException ignored) {}

                // Stored rewards (virtual backpack) — stored as a pipe-delimited
                // string in the stored_rewards TEXT column. Format per entry:
                //   MATERIAL|data|name|lore1;;lore2;;lore3|cmd1;;cmd2;;cmd3
                // Entries are separated by "||". This avoids needing a JSON lib.
                try {
                    String rewardsRaw = rs.getString("stored_rewards");
                    if (rewardsRaw != null && !rewardsRaw.isEmpty()) {
                        deserializeRewards(data, rewardsRaw);
                    }
                } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
        }
        return data;
    }

    private void saveToDatabase(PlayerData data) {
        StringBuilder claimed = new StringBuilder();
        for (String key : data.getClaimedTiers()) {
            if (claimed.length() > 0) claimed.append(",");
            claimed.append(key);
        }

        String sql = "INSERT INTO wasteland_players "
                + "(uuid,mining_level,mining_xp,"
                + "woodcutting_level,woodcutting_xp,"
                + "farming_level,farming_xp,"
                + "fishing_level,fishing_xp,"
                + "tier,claimed_tiers,playtime_seconds,stored_rewards,"
                + "settings_see_players,settings_xp_noises,settings_xp_bar_display) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "mining_level=VALUES(mining_level),mining_xp=VALUES(mining_xp),"
                + "woodcutting_level=VALUES(woodcutting_level),woodcutting_xp=VALUES(woodcutting_xp),"
                + "farming_level=VALUES(farming_level),farming_xp=VALUES(farming_xp),"
                + "fishing_level=VALUES(fishing_level),fishing_xp=VALUES(fishing_xp),"
                + "tier=VALUES(tier),claimed_tiers=VALUES(claimed_tiers),"
                + "playtime_seconds=VALUES(playtime_seconds),stored_rewards=VALUES(stored_rewards),"
                + "settings_see_players=VALUES(settings_see_players),"
                + "settings_xp_noises=VALUES(settings_xp_noises),"
                + "settings_xp_bar_display=VALUES(settings_xp_bar_display)";

        String rewardsSerialized = serializeRewards(data);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getUuid().toString());
            stmt.setInt(2,  data.getLevel(SkillType.MINING));
            stmt.setLong(3, data.getXp(SkillType.MINING));
            stmt.setInt(4,  data.getLevel(SkillType.WOODCUTTING));
            stmt.setLong(5, data.getXp(SkillType.WOODCUTTING));
            stmt.setInt(6,  data.getLevel(SkillType.FARMING));
            stmt.setLong(7, data.getXp(SkillType.FARMING));
            stmt.setInt(8,  data.getLevel(SkillType.FISHING));
            stmt.setLong(9, data.getXp(SkillType.FISHING));
            stmt.setInt(10, data.getTier());
            stmt.setString(11, claimed.toString());
            stmt.setLong(12, data.getPlaytimeSeconds());
            stmt.setString(13, rewardsSerialized);
            stmt.setBoolean(14, data.isSettingSeePlayers());
            stmt.setBoolean(15, data.isSettingXpNoises());
            stmt.setBoolean(16, data.isSettingXpBarDisplay());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
        }
    }

    /**
     * Serialize the player's stored rewards into a single TEXT-friendly string.
     * Format per entry: MATERIAL|data|name|lore1;;lore2|cmd1;;cmd2
     * Entries separated by "||". Pipe characters inside lore/commands are
     * replaced with a Unicode lookalike to avoid breaking the format.
     */
    private String serializeRewards(PlayerData data) {
        StringBuilder sb = new StringBuilder();
        for (StoredReward r : data.getStoredRewards()) {
            if (sb.length() > 0) sb.append("||");
            sb.append(r.getDisplayMaterial().name()).append("|");
            sb.append((int) r.getDisplayData()).append("|");
            sb.append(escape(r.getDisplayName())).append("|");
            sb.append(joinEscaped(r.getDisplayLore())).append("|");
            sb.append(joinEscaped(r.getCommands()));
        }
        return sb.toString();
    }

    /** Deserialize the pipe-delimited reward string and populate the player's data. */
    private void deserializeRewards(PlayerData data, String raw) {
        if (raw == null || raw.isEmpty()) return;
        String[] entries = raw.split("\\|\\|");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String[] parts = entry.split("\\|", -1);
            if (parts.length < 5) continue;
            try {
                Material mat;
                try { mat = Material.valueOf(parts[0].toUpperCase()); }
                catch (Exception ex) { mat = Material.CHEST; }
                short dataVal = Short.parseShort(parts[1]);
                String name = unescape(parts[2]);
                List<String> lore = splitEscaped(parts[3]);
                List<String> commands = splitEscaped(parts[4]);
                data.addStoredReward(new StoredReward(mat, dataVal, name, lore, commands));
            } catch (Exception ignored) {
                // Skip malformed entries.
            }
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("|", "\u2502").replace(";;", "\u2044\u2044");
    }

    private String unescape(String s) {
        return s == null ? "" : s.replace("\u2502", "|").replace("\u2044\u2044", ";;");
    }

    private String joinEscaped(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(";;");
            sb.append(escape(s));
        }
        return sb.toString();
    }

    private List<String> splitEscaped(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(";;", -1)) {
            out.add(unescape(part));
        }
        return out;
    }
}
