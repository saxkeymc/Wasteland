package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;

/**
 * Manages the Dust currency used to upgrade Omni Tools.
 * <p>
 * Players earn Dust from mining, chopping, farming, and fishing.
 * Dust is used to upgrade tools, which is required to mine blocks
 * at higher tiers (Tier 2+ requires tool upgrades).
 * <p>
 * Upgrade costs per level:
 * <ul>
 *   <li>Upgrade 1: 500 dust</li>
 *   <li>Upgrade 2: 1000 dust</li>
 *   <li>Upgrade 3: 1500 dust</li>
 *   <li>Upgrade 4: 2000 dust</li>
 * </ul>
 * <p>
 * Tool upgrade level corresponds to tier: a Tier 2 tool requires
 * upgrade 1, Tier 3 requires upgrade 2, etc.
 */
public class DustManager {

    private final WastelandPlugin plugin;

    /** Dust cost per upgrade level (index 0 = upgrade 1). */
    private static final int[] UPGRADE_COSTS = {500, 1000, 1500, 2000};

    /** Dust earned per action by default. */
    private static final int DEFAULT_DUST_PER_ACTION = 5;

    public DustManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get the required tool upgrade level for a given tier.
     * Tier 1 = no upgrade needed (0).
     * Tier 2 = upgrade 1.
     * Tier 3 = upgrade 2.
     * Tier 4 = upgrade 3.
     * Tier 5 = upgrade 4.
     */
    public int getRequiredUpgradeLevel(int tier) {
        if (tier <= 1) return 0;
        return tier - 1; // Tier 2 → 1, Tier 3 → 2, etc.
    }

    /**
     * Get the dust cost for a specific upgrade level (1-indexed).
     */
    public int getUpgradeCost(int upgradeLevel) {
        if (upgradeLevel < 1 || upgradeLevel > UPGRADE_COSTS.length) return Integer.MAX_VALUE;
        return UPGRADE_COSTS[upgradeLevel - 1];
    }

    /**
     * Check if the player's tool for the given skill is upgraded enough
     * to work at the given tier.
     */
    public boolean hasRequiredUpgrade(PlayerData data, SkillType skill, int tier) {
        int required = getRequiredUpgradeLevel(tier);
        int current = data.getToolUpgradeLevel(skill);
        return current >= required;
    }

    /**
     * Attempt to upgrade the player's tool for the given skill.
     * Returns true if successful, false if not enough dust or already maxed.
     */
    public boolean upgradeTool(PlayerData data, SkillType skill) {
        int currentLevel = data.getToolUpgradeLevel(skill);
        int nextLevel = currentLevel + 1;
        if (nextLevel > UPGRADE_COSTS.length) return false; // maxed

        int cost = getUpgradeCost(nextLevel);
        if (data.getDust() < cost) return false; // not enough dust

        data.setDust(data.getDust() - cost);
        data.setToolUpgradeLevel(skill, nextLevel);
        return true;
    }

    /**
     * Award dust to a player. Called when they mine/chop/farm/fish.
     */
    public void awardDust(Player player, int amount) {
        if (amount <= 0) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.addDust(amount);
        plugin.getDataManager().savePlayer(player.getUniqueId());
    }

    /**
     * Get the default dust amount for a skill action.
     */
    public int getDefaultDustPerAction(SkillType skill) {
        // Fishing gives more dust, farming gives less — matches XP hierarchy.
        switch (skill) {
            case FISHING:  return 15;
            case MINING:   return 10;
            case WOODCUTTING: return 7;
            case FARMING:  return 5;
            default: return DEFAULT_DUST_PER_ACTION;
        }
    }
}
