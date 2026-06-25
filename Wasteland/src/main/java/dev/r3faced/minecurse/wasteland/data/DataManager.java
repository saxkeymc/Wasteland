package dev.r3faced.minecurse.wasteland.data;

import dev.r3faced.minecurse.wasteland.model.PlayerData;

import java.util.UUID;

/**
 * Abstraction layer for persistent data storage.
 * Implementations: YamlDataManager, MySQLDataManager.
 */
public interface DataManager {

    /**
     * Initialize the data backend (create tables, directories, etc.).
     */
    void init();

    /**
     * Retrieve player data, loading from disk/database if not cached.
     * Creates a new default record if no data exists.
     *
     * @param uuid the player's UUID
     * @return the player's WastelandData
     */
    PlayerData getPlayerData(UUID uuid);

    /**
     * Asynchronously save the given player's data to disk/database.
     * Falls back to synchronous save when the plugin is disabled
     * (e.g. during onDisable) to avoid IllegalPluginAccessException.
     *
     * @param uuid the player's UUID
     */
    void savePlayer(UUID uuid);

    /**
     * Synchronously save the given player's data.
     * Used during shutdown / reload to guarantee data is persisted
     * before the plugin unloads. NEVER uses the Bukkit scheduler.
     *
     * @param uuid the player's UUID
     */
    void savePlayerSync(UUID uuid);

    /**
     * Synchronously flush all dirty records to storage. Used on shutdown.
     */
    void saveAll();

    /**
     * Remove a player's data from the in-memory cache.
     * Should be called on PlayerQuitEvent after saving.
     *
     * @param uuid the player's UUID
     */
    void unloadPlayer(UUID uuid);

    /**
     * Reset all data for a player back to defaults.
     *
     * @param uuid the player's UUID
     */
    void resetPlayer(UUID uuid);

    /**
     * Shut down any connections or thread pools gracefully.
     */
    void shutdown();
}
