package dev.r3faced.minecurse.wasteland.api;

import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Convenience wrapper around a Wasteland player's data.
 */
public class WastelandPlayer {

    private final WastelandApi api;
    private final UUID uuid;

    public WastelandPlayer(WastelandApi api, UUID uuid) {
        if (api == null) {
            throw new IllegalArgumentException("api cannot be null");
        }
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        this.api = api;
        this.uuid = uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        Player player = getBukkitPlayer();
        return player != null && player.isOnline();
    }

    public PlayerData getRawData() {
        return api.getPlayerData(uuid);
    }

    public int getLevel(SkillType skill) {
        return api.getLevel(uuid, skill);
    }

    public Map<SkillType, Integer> getLevels() {
        return api.getLevels(uuid);
    }

    public long getXp(SkillType skill) {
        return api.getXp(uuid, skill);
    }

    public Map<SkillType, Long> getXpMap() {
        return api.getXpMap(uuid);
    }

    public int getTier() {
        return api.getTier(uuid);
    }

    public long getPlaytimeSeconds() {
        return api.getPlaytimeSeconds(uuid);
    }

    public List<StoredReward> getStoredRewards() {
        return api.getStoredRewards(uuid);
    }

    public long addXp(SkillType skill, long amount, WastelandXpCause cause, String source) {
        return api.addXp(requireOnline(), skill, amount, cause, source);
    }

    public long removeXp(SkillType skill, long amount, WastelandChangeReason reason) {
        return api.removeXp(requireOnline(), skill, amount, reason);
    }

    public long setXp(SkillType skill, long xp, WastelandChangeReason reason) {
        return api.setXp(requireOnline(), skill, xp, reason);
    }

    public int setLevel(SkillType skill, int level, WastelandChangeReason reason) {
        return api.setLevel(requireOnline(), skill, level, reason);
    }

    public int setTier(int tier, WastelandChangeReason reason) {
        return api.setTier(requireOnline(), tier, reason);
    }

    public void save() {
        api.savePlayer(uuid);
    }

    private Player requireOnline() {
        Player player = getBukkitPlayer();
        if (player == null || !player.isOnline()) {
            throw new IllegalStateException("Player " + uuid + " is not online.");
        }
        return player;
    }
}
