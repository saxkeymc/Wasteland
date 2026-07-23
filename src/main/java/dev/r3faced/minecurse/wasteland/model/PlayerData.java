package dev.r3faced.minecurse.wasteland.model;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class PlayerData {

    private final UUID uuid;

    private final Map<SkillType, Integer> levels = new EnumMap<>(SkillType.class);

    private final Map<SkillType, Long> xp = new EnumMap<>(SkillType.class);

    private int tier = 1;

    private final Set<String> claimedTiers = new HashSet<>();

    private long playtimeSeconds = 0L;

    private boolean settingSeePlayers = true;
    private boolean settingXpNoises = true;
    private boolean settingXpBarDisplay = true;

    private transient int savedVanillaLevel = 0;
    private transient float savedVanillaXp = 0f;
    private transient SkillType activeSkill = null;

    private int dust = 0;

    private final Map<SkillType, Integer> toolUpgrades = new EnumMap<>(SkillType.class);

    private final List<ItemStack> backpackItems = new java.util.ArrayList<>();

    private final List<StoredReward> storedRewards = new java.util.ArrayList<>();

    private transient long inWastelandSince = 0L;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (SkillType skill : SkillType.values()) {
            levels.put(skill, 0);
            xp.put(skill, 0L);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel(SkillType skill) {
        return levels.getOrDefault(skill, 0);
    }

    public void setLevel(SkillType skill, int level) {
        levels.put(skill, level);
    }

    public int getTotalLevel() {
        int total = 0;
        for (SkillType skill : SkillType.values()) {
            total += getLevel(skill);
        }
        return total;
    }

    public long getXp(SkillType skill) {
        return xp.getOrDefault(skill, 0L);
    }

    public void setXp(SkillType skill, long amount) {
        xp.put(skill, amount);
    }

    public void addXp(SkillType skill, long amount) {
        xp.put(skill, getXp(skill) + amount);
    }

    public long getTotalXp() {
        long total = 0L;
        for (SkillType skill : SkillType.values()) {
            total += getXp(skill);
        }
        return total;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public static String tierClaimKey(int tier) {
        return "tier_" + tier;
    }

    public boolean hasClaimedTier(int tier) {
        return claimedTiers.contains(tierClaimKey(tier));
    }

    public void claimTier(int tier) {
        claimedTiers.add(tierClaimKey(tier));
    }

    public Set<String> getClaimedTiers() {
        return claimedTiers;
    }

    public void setClaimedTiers(Set<String> keys) {
        claimedTiers.clear();
        claimedTiers.addAll(keys);
    }

    public long getPlaytimeSeconds() {
        return playtimeSeconds;
    }

    public void setPlaytimeSeconds(long seconds) {
        this.playtimeSeconds = seconds;
    }

    public void addPlaytimeSeconds(long seconds) {
        this.playtimeSeconds += seconds;
    }

    public long getInWastelandSince() {
        return inWastelandSince;
    }

    public void setInWastelandSince(long millis) {
        this.inWastelandSince = millis;
    }

    public List<StoredReward> getStoredRewards() {
        return storedRewards;
    }

    public void addStoredReward(StoredReward reward) {
        if (reward != null) {
            storedRewards.add(reward);
        }
    }

    public void addStoredRewards(List<StoredReward> rewards) {
        if (rewards != null) {
            for (StoredReward r : rewards) {
                if (r != null) storedRewards.add(r);
            }
        }
    }

    public void removeStoredReward(StoredReward reward) {
        storedRewards.remove(reward);
    }

    public void clearStoredRewards() {
        storedRewards.clear();
    }

    public Map<SkillType, Integer> getLevels() {
        return levels;
    }

    public Map<SkillType, Long> getXpMap() {
        return xp;
    }

    public boolean isSettingSeePlayers() { return settingSeePlayers; }
    public void setSettingSeePlayers(boolean v) { this.settingSeePlayers = v; }

    public boolean isSettingXpNoises() { return settingXpNoises; }
    public void setSettingXpNoises(boolean v) { this.settingXpNoises = v; }

    public boolean isSettingXpBarDisplay() { return settingXpBarDisplay; }
    public void setSettingXpBarDisplay(boolean v) { this.settingXpBarDisplay = v; }

    public int getSavedVanillaLevel() { return savedVanillaLevel; }
    public void setSavedVanillaLevel(int v) { this.savedVanillaLevel = v; }

    public float getSavedVanillaXp() { return savedVanillaXp; }
    public void setSavedVanillaXp(float v) { this.savedVanillaXp = v; }

    public SkillType getActiveSkill() { return activeSkill; }
    public void setActiveSkill(SkillType skill) { this.activeSkill = skill; }

    public int getDust() { return dust; }
    public void setDust(int v) { this.dust = v; }
    public void addDust(int amount) { this.dust += amount; }

    public int getToolUpgradeLevel(SkillType skill) {
        return toolUpgrades.getOrDefault(skill, 0);
    }
    public void setToolUpgradeLevel(SkillType skill, int level) {
        toolUpgrades.put(skill, level);
    }
    public Map<SkillType, Integer> getToolUpgrades() { return toolUpgrades; }

    public List<ItemStack> getBackpackItems() { return backpackItems; }
    public void clearBackpack() { backpackItems.clear(); }
}
