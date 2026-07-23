package dev.r3faced.minecurse.wasteland.data;

import dev.r3faced.minecurse.wasteland.model.PlayerData;

import java.util.UUID;

public interface DataManager {

    void init();

    PlayerData getPlayerData(UUID uuid);

    void savePlayer(UUID uuid);

    void savePlayerSync(UUID uuid);

    void saveAll();

    void unloadPlayer(UUID uuid);

    void resetPlayer(UUID uuid);

    void shutdown();
}
