package dev.r3faced.minecurse.wasteland.api.internal;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandApi;
import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.api.WastelandPlayer;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.api.event.WastelandPlayerResetEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandSkillChangeEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandSkillLevelUpEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandStoredRewardAddEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandTierChangeEvent;
import dev.r3faced.minecurse.wasteland.gui.menus.CollectMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.HelpMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.MainMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.RewardPageMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.SkillMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.StatsMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.TierMenuGui;
import dev.r3faced.minecurse.wasteland.managers.TierManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import dev.r3faced.minecurse.wasteland.model.TierReward;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WastelandApiImpl implements WastelandApi {

    private final WastelandPlugin plugin;

    public WastelandApiImpl(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public WastelandPlugin getPlugin() {
        return plugin;
    }

    @Override
    public WastelandPlayer getPlayer(UUID uuid) {
        requireUuid(uuid);
        return new WastelandPlayer(this, uuid);
    }

    @Override
    public WastelandPlayer getPlayer(Player player) {
        requirePlayer(player);
        return getPlayer(player.getUniqueId());
    }

    @Override
    public PlayerData getPlayerData(UUID uuid) {
        requireUuid(uuid);
        return plugin.getDataManager().getPlayerData(uuid);
    }

    @Override
    public void savePlayer(UUID uuid) {
        requireUuid(uuid);
        plugin.getDataManager().savePlayer(uuid);
    }

    @Override
    public void savePlayerSync(UUID uuid) {
        requireUuid(uuid);
        plugin.getDataManager().savePlayerSync(uuid);
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        requireUuid(uuid);
        plugin.getDataManager().unloadPlayer(uuid);
    }

    @Override
    public boolean resetPlayer(UUID uuid) {
        return resetPlayer(uuid, WastelandChangeReason.API);
    }

    @Override
    public boolean resetPlayer(UUID uuid, WastelandChangeReason reason) {
        requireUuid(uuid);
        WastelandPlayerResetEvent event = new WastelandPlayerResetEvent(uuid, safeReason(reason));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        plugin.getDataManager().resetPlayer(uuid);
        return true;
    }

    @Override
    public int getLevel(UUID uuid, SkillType skill) {
        return getPlayerData(uuid).getLevel(requireSkill(skill));
    }

    @Override
    public Map<SkillType, Integer> getLevels(UUID uuid) {
        Map<SkillType, Integer> copy = new EnumMap<>(SkillType.class);
        copy.putAll(getPlayerData(uuid).getLevels());
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public int setLevel(Player player, SkillType skill, int level) {
        return setLevel(player, skill, level, WastelandChangeReason.API);
    }

    @Override
    public int setLevel(Player player, SkillType skill, int level, WastelandChangeReason reason) {
        requirePlayer(player);
        requireSkill(skill);
        int capped = clamp(level, 0, plugin.getSkillManager().getLevelCap(skill));
        setXp(player, skill, plugin.getSkillManager().xpRequiredForLevel(skill, capped), reason);
        return capped;
    }

    @Override
    public long getXp(UUID uuid, SkillType skill) {
        return getPlayerData(uuid).getXp(requireSkill(skill));
    }

    @Override
    public Map<SkillType, Long> getXpMap(UUID uuid) {
        Map<SkillType, Long> copy = new EnumMap<>(SkillType.class);
        copy.putAll(getPlayerData(uuid).getXpMap());
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public long addXp(Player player, SkillType skill, long amount) {
        return addXp(player, skill, amount, WastelandXpCause.API, "api");
    }

    @Override
    public long addXp(Player player, SkillType skill, long amount, WastelandXpCause cause, String source) {
        requirePlayer(player);
        requireSkill(skill);
        return plugin.getSkillManager().awardXp(player, skill, amount, safeCause(cause), source);
    }

    @Override
    public long removeXp(Player player, SkillType skill, long amount) {
        return removeXp(player, skill, amount, WastelandChangeReason.API);
    }

    @Override
    public long removeXp(Player player, SkillType skill, long amount, WastelandChangeReason reason) {
        requirePlayer(player);
        requireSkill(skill);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        long newXp = Math.max(0L, data.getXp(skill) - Math.max(0L, amount));
        return setXp(player, skill, newXp, reason);
    }

    @Override
    public long setXp(Player player, SkillType skill, long xp) {
        return setXp(player, skill, xp, WastelandChangeReason.API);
    }

    @Override
    public long setXp(Player player, SkillType skill, long xp, WastelandChangeReason reason) {
        requirePlayer(player);
        requireSkill(skill);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int oldLevel = data.getLevel(skill);
        long oldXp = data.getXp(skill);

        long newXp = Math.max(0L, xp);
        int newLevel = calculateLevelFromXp(skill, newXp);

        if (oldLevel == newLevel && oldXp == newXp) {
            return newXp;
        }

        data.setXp(skill, newXp);
        data.setLevel(skill, newLevel);
        plugin.getDataManager().savePlayer(player.getUniqueId());

        WastelandChangeReason safeReason = safeReason(reason);
        Bukkit.getPluginManager().callEvent(new WastelandSkillChangeEvent(
                player, skill, oldLevel, newLevel, oldXp, newXp, safeReason));

        if (newLevel > oldLevel) {
            WastelandXpCause cause = reasonToCause(safeReason);
            for (int level = oldLevel + 1; level <= newLevel; level++) {
                Bukkit.getPluginManager().callEvent(new WastelandSkillLevelUpEvent(
                        player, skill, level - 1, level, cause, "set-xp"));
            }
            plugin.getTierManager().checkTierUnlock(player);
        }

        return newXp;
    }

    @Override
    public int getLevelCap(SkillType skill) {
        return plugin.getSkillManager().getLevelCap(requireSkill(skill));
    }

    @Override
    public int getXpForMaterial(SkillType skill, String materialName) {
        requireSkill(skill);
        return plugin.getSkillManager().getXpForBlock(skill, materialName == null ? "DEFAULT" : materialName.toUpperCase());
    }

    @Override
    public long getXpRequiredForLevel(SkillType skill, int level) {
        return plugin.getSkillManager().xpRequiredForLevel(requireSkill(skill), level);
    }

    @Override
    public long getXpToNextLevel(SkillType skill, int currentLevel) {
        return plugin.getSkillManager().xpToNextLevel(requireSkill(skill), currentLevel);
    }

    @Override
    public String getProgressBar(UUID uuid, SkillType skill) {
        return plugin.getSkillManager().getProgressBar(requireSkill(skill), getPlayerData(uuid));
    }

    @Override
    public int getTier(UUID uuid) {
        return getPlayerData(uuid).getTier();
    }

    @Override
    public int setTier(Player player, int tier) {
        return setTier(player, tier, WastelandChangeReason.API);
    }

    @Override
    public int setTier(Player player, int tier, WastelandChangeReason reason) {
        requirePlayer(player);
        int capped = clamp(tier, 1, TierManager.TIER_COUNT);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int oldTier = data.getTier();
        if (oldTier == capped) {
            return capped;
        }
        data.setTier(capped);
        plugin.getDataManager().savePlayer(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new WastelandTierChangeEvent(
                player, oldTier, capped, safeReason(reason)));
        return capped;
    }

    @Override
    public int getTierCount() {
        return TierManager.TIER_COUNT;
    }

    @Override
    public int getRequiredLevel(int tier) {
        return plugin.getTierManager().getRequiredLevel(tier);
    }

    @Override
    public boolean meetsTierRequirements(UUID uuid, int tier) {
        return plugin.getTierManager().meetsRequirements(getPlayerData(uuid), tier);
    }

    @Override
    public List<SkillType> getMissingSkills(UUID uuid, int tier) {
        return Collections.unmodifiableList(new ArrayList<>(plugin.getTierManager().getMissingSkills(getPlayerData(uuid), tier)));
    }

    @Override
    public void checkTierUnlocks(Player player) {
        requirePlayer(player);
        plugin.getTierManager().checkTierUnlock(player);
    }

    @Override
    public List<TierReward> getTierRewards(int tier) {
        return Collections.unmodifiableList(new ArrayList<>(plugin.getTierManager().getRewards(tier)));
    }

    @Override
    public boolean claimTierRewards(Player player, int tier) {
        requirePlayer(player);
        return plugin.getTierManager().dispatchRewards(player, tier);
    }

    @Override
    public List<StoredReward> getStoredRewards(UUID uuid) {
        return Collections.unmodifiableList(new ArrayList<>(getPlayerData(uuid).getStoredRewards()));
    }

    @Override
    public boolean addStoredReward(Player player, StoredReward reward) {
        return addStoredReward(player, reward, WastelandChangeReason.API);
    }

    @Override
    public boolean addStoredReward(Player player, StoredReward reward, WastelandChangeReason reason) {
        requirePlayer(player);
        requireReward(reward);

        WastelandStoredRewardAddEvent event = new WastelandStoredRewardAddEvent(player, reward, -1, safeReason(reason));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.addStoredReward(event.getReward());
        plugin.getDataManager().savePlayer(player.getUniqueId());
        return true;
    }

    @Override
    public boolean removeStoredReward(UUID uuid, StoredReward reward) {
        requireUuid(uuid);
        requireReward(reward);
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        boolean removed = data.getStoredRewards().remove(reward);
        if (removed) {
            plugin.getDataManager().savePlayer(uuid);
        }
        return removed;
    }

    @Override
    public boolean claimStoredReward(Player player, StoredReward reward) {
        requirePlayer(player);
        requireReward(reward);
        return plugin.getTierManager().claimStoredReward(player, reward);
    }

    @Override
    public long getPlaytimeSeconds(UUID uuid) {
        return getPlayerData(uuid).getPlaytimeSeconds();
    }

    @Override
    public void setPlaytimeSeconds(UUID uuid, long seconds) {
        PlayerData data = getPlayerData(uuid);
        data.setPlaytimeSeconds(Math.max(0L, seconds));
        plugin.getDataManager().savePlayer(uuid);
    }

    @Override
    public void addPlaytimeSeconds(UUID uuid, long seconds) {
        if (seconds <= 0L) {
            return;
        }
        PlayerData data = getPlayerData(uuid);
        data.addPlaytimeSeconds(seconds);
        plugin.getDataManager().savePlayer(uuid);
    }

    @Override
    public ItemStack buildOmniTool(Player player, SkillType skill) {
        requirePlayer(player);
        return buildOmniTool(player.getUniqueId(), skill);
    }

    @Override
    public ItemStack buildOmniTool(UUID uuid, SkillType skill) {
        return plugin.getToolManager().buildOmniTool(requireSkill(skill), getPlayerData(uuid));
    }

    @Override
    public void giveOmniTool(Player player, SkillType skill) {
        requirePlayer(player);
        plugin.getToolManager().giveOmniTool(player, requireSkill(skill));
    }

    @Override
    public boolean isOmniTool(ItemStack item) {
        return plugin.getToolManager().getToolSkill(item) != null;
    }

    @Override
    public boolean isOmniTool(ItemStack item, SkillType skill) {
        return plugin.getToolManager().isOmniTool(item, requireSkill(skill));
    }

    @Override
    public SkillType getToolSkill(ItemStack item) {
        return plugin.getToolManager().getToolSkill(item);
    }

    @Override
    public SkillType getSkillForWorld(String worldName) {
        return plugin.getToolManager().getSkillForWorld(worldName);
    }

    @Override
    public SkillType getSkillForWorld(World world) {
        return world == null ? null : getSkillForWorld(world.getName());
    }

    @Override
    public boolean isWastelandWorld(String worldName) {
        return plugin.getWastelandWorldManager().isWastelandWorld(worldName);
    }

    @Override
    public boolean isWastelandWorld(World world) {
        return plugin.getWastelandWorldManager().isWastelandWorld(world);
    }

    @Override
    public Set<String> getWastelandWorlds() {
        return Collections.unmodifiableSet(plugin.getWastelandWorldManager().getWastelandWorlds());
    }

    @Override
    public void openMainMenu(Player player) {
        requirePlayer(player);
        new MainMenuGui(plugin, player).open();
    }

    @Override
    public void openStatsMenu(Player player) {
        requirePlayer(player);
        new StatsMenuGui(plugin, player).open();
    }

    @Override
    public void openSkillMenu(Player player, SkillType skill) {
        requirePlayer(player);
        new SkillMenuGui(plugin, player, requireSkill(skill)).open();
    }

    @Override
    public void openTierMenu(Player player) {
        requirePlayer(player);
        new TierMenuGui(plugin, player).open();
    }

    @Override
    public void openTierRewardsMenu(Player player, int tier) {
        requirePlayer(player);
        new RewardPageMenuGui(plugin, player, clamp(tier, 1, TierManager.TIER_COUNT), 0).open();
    }

    @Override
    public void openCollectMenu(Player player) {
        requirePlayer(player);
        new CollectMenuGui(plugin, player).open();
    }

    @Override
    public void openHelpMenu(Player player) {
        requirePlayer(player);
        new HelpMenuGui(plugin, player).open();
    }

    private int calculateLevelFromXp(SkillType skill, long xp) {
        int newLevel = 0;
        int cap = plugin.getSkillManager().getLevelCap(skill);
        while (newLevel < cap && plugin.getSkillManager().xpRequiredForLevel(skill, newLevel + 1) <= xp) {
            newLevel++;
        }
        return newLevel;
    }

    private WastelandChangeReason safeReason(WastelandChangeReason reason) {
        return reason == null ? WastelandChangeReason.API : reason;
    }

    private WastelandXpCause safeCause(WastelandXpCause cause) {
        return cause == null ? WastelandXpCause.API : cause;
    }

    private WastelandXpCause reasonToCause(WastelandChangeReason reason) {
        switch (reason) {
            case GAMEPLAY:
                return WastelandXpCause.CUSTOM;
            case COMMAND:
                return WastelandXpCause.COMMAND;
            case API:
                return WastelandXpCause.API;
            case REWARD:
            case RESET:
            case CUSTOM:
            default:
                return WastelandXpCause.CUSTOM;
        }
    }

    private void requirePlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }
    }

    private UUID requireUuid(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        return uuid;
    }

    private SkillType requireSkill(SkillType skill) {
        if (skill == null) {
            throw new IllegalArgumentException("skill cannot be null");
        }
        return skill;
    }

    private StoredReward requireReward(StoredReward reward) {
        if (reward == null) {
            throw new IllegalArgumentException("reward cannot be null");
        }
        return reward;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
