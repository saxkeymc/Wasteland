package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;

public class DustManager {

    private final WastelandPlugin plugin;

    private static final int[] UPGRADE_COSTS = {500, 1000, 1500, 2000};

    private static final int DEFAULT_DUST_PER_ACTION = 5;

    public DustManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    public int getRequiredUpgradeLevel(int tier) {
        if (tier <= 1) return 0;
        return tier - 1;
    }

    public int getUpgradeCost(int upgradeLevel) {
        if (upgradeLevel < 1 || upgradeLevel > UPGRADE_COSTS.length) return Integer.MAX_VALUE;
        return UPGRADE_COSTS[upgradeLevel - 1];
    }

    public boolean hasRequiredUpgrade(PlayerData data, SkillType skill, int tier) {
        int required = getRequiredUpgradeLevel(tier);
        int current = data.getToolUpgradeLevel(skill);
        return current >= required;
    }

    public boolean upgradeTool(PlayerData data, SkillType skill) {
        int currentLevel = data.getToolUpgradeLevel(skill);
        int nextLevel = currentLevel + 1;
        if (nextLevel > UPGRADE_COSTS.length) return false;

        int cost = getUpgradeCost(nextLevel);
        if (data.getDust() < cost) return false;

        data.setDust(data.getDust() - cost);
        data.setToolUpgradeLevel(skill, nextLevel);
        return true;
    }

    public void awardDust(Player player, int amount) {
        if (amount <= 0) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.addDust(amount);
        plugin.getDataManager().savePlayer(player.getUniqueId());
    }

    public int getDefaultDustPerAction(SkillType skill) {
        switch (skill) {
            case FISHING:  return 15;
            case MINING:   return 10;
            case WOODCUTTING: return 7;
            case FARMING:  return 5;
            default: return DEFAULT_DUST_PER_ACTION;
        }
    }
}
