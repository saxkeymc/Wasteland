package dev.r3faced.minecurse.wasteland.api;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import dev.r3faced.minecurse.wasteland.model.TierReward;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface WastelandApi {

    WastelandPlugin getPlugin();

    WastelandPlayer getPlayer(UUID uuid);

    WastelandPlayer getPlayer(Player player);

    PlayerData getPlayerData(UUID uuid);

    void savePlayer(UUID uuid);

    void savePlayerSync(UUID uuid);

    void unloadPlayer(UUID uuid);

    boolean resetPlayer(UUID uuid);

    boolean resetPlayer(UUID uuid, WastelandChangeReason reason);

    int getLevel(UUID uuid, SkillType skill);

    Map<SkillType, Integer> getLevels(UUID uuid);

    int setLevel(Player player, SkillType skill, int level);

    int setLevel(Player player, SkillType skill, int level, WastelandChangeReason reason);

    long getXp(UUID uuid, SkillType skill);

    Map<SkillType, Long> getXpMap(UUID uuid);

    long addXp(Player player, SkillType skill, long amount);

    long addXp(Player player, SkillType skill, long amount, WastelandXpCause cause, String source);

    long removeXp(Player player, SkillType skill, long amount);

    long removeXp(Player player, SkillType skill, long amount, WastelandChangeReason reason);

    long setXp(Player player, SkillType skill, long xp);

    long setXp(Player player, SkillType skill, long xp, WastelandChangeReason reason);

    int getLevelCap(SkillType skill);

    int getXpForMaterial(SkillType skill, String materialName);

    long getXpRequiredForLevel(SkillType skill, int level);

    long getXpToNextLevel(SkillType skill, int currentLevel);

    String getProgressBar(UUID uuid, SkillType skill);

    int getTier(UUID uuid);

    int setTier(Player player, int tier);

    int setTier(Player player, int tier, WastelandChangeReason reason);

    int getTierCount();

    int getRequiredLevel(int tier);

    boolean meetsTierRequirements(UUID uuid, int tier);

    List<SkillType> getMissingSkills(UUID uuid, int tier);

    void checkTierUnlocks(Player player);

    List<TierReward> getTierRewards(int tier);

    boolean claimTierRewards(Player player, int tier);

    List<StoredReward> getStoredRewards(UUID uuid);

    boolean addStoredReward(Player player, StoredReward reward);

    boolean addStoredReward(Player player, StoredReward reward, WastelandChangeReason reason);

    boolean removeStoredReward(UUID uuid, StoredReward reward);

    boolean claimStoredReward(Player player, StoredReward reward);

    long getPlaytimeSeconds(UUID uuid);

    void setPlaytimeSeconds(UUID uuid, long seconds);

    void addPlaytimeSeconds(UUID uuid, long seconds);

    ItemStack buildOmniTool(Player player, SkillType skill);

    ItemStack buildOmniTool(UUID uuid, SkillType skill);

    void giveOmniTool(Player player, SkillType skill);

    boolean isOmniTool(ItemStack item);

    boolean isOmniTool(ItemStack item, SkillType skill);

    SkillType getToolSkill(ItemStack item);

    SkillType getSkillForWorld(String worldName);

    SkillType getSkillForWorld(World world);

    boolean isWastelandWorld(String worldName);

    boolean isWastelandWorld(World world);

    Set<String> getWastelandWorlds();

    void openMainMenu(Player player);

    void openStatsMenu(Player player);

    void openSkillMenu(Player player, SkillType skill);

    void openTierMenu(Player player);

    void openTierRewardsMenu(Player player, int tier);

    void openCollectMenu(Player player);

    void openHelpMenu(Player player);
}
