package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.TierReward;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the SHARED tier system.
 * <p>
 * There is only ONE tier progression per player — every skill contributes
 * toward unlocking the same tiers. There are no "Mining Tier", "Fishing
 * Tier", etc. Each tier has a single shared reward pool.
 * <p>
 * Tier unlock is based on the player's TOTAL level across all four skills.
 * <p>
 * Reward config format (tiers.yml):
 * <pre>
 * tiers:
 *   1:
 *     required-level: 0
 *     display-name: "&aTier 1"
 *     glass-color: STAINED_GLASS_PANE
 *     glass-data: 5
 *     rewards:
 *       reward_1:
 *         chance: 100
 *         display-item:
 *           material: CHEST
 *           name: "&fStarter Reward"
 *           lore:
 *             - "&7A small bonus."
 *         commands:
 *           - "eco give %player% 1000"
 * </pre>
 * <p>
 * Commands are NEVER exposed to the GUI. The GUI only sees display-item.
 */
public class TierManager {

    private final WastelandPlugin plugin;

    /** Total number of tiers. */
    public static final int TIER_COUNT = 4;

    /** Level required to unlock each tier (based on total level). */
    private final Map<Integer, Integer> tierRequirements = new HashMap<>();

    /** Rewards per tier (shared across all skills). */
    private final Map<Integer, List<TierReward>> tierRewards = new HashMap<>();

    public TierManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tierRequirements.clear();
        tierRewards.clear();

        FileConfiguration cfg = plugin.getConfigManager().getTiers();

        for (int tier = 1; tier <= TIER_COUNT; tier++) {
            String path = "tiers." + tier;
            tierRequirements.put(tier, cfg.getInt(path + ".required-level", (tier - 1) * 10));
            tierRewards.put(tier, parseRewards(cfg, path + ".rewards"));
        }
    }

    /**
     * Parse the reward-format configuration block into a list of TierReward objects.
     * Falls back gracefully: missing sections produce an empty list.
     */
    private List<TierReward> parseRewards(FileConfiguration cfg, String basePath) {
        List<TierReward> out = new ArrayList<>();
        if (!cfg.isConfigurationSection(basePath)) return out;

        ConfigurationSection parent = cfg.getConfigurationSection(basePath);
        for (String rewardKey : parent.getKeys(false)) {
            String rewardPath = basePath + "." + rewardKey;

            double chance = cfg.getDouble(rewardPath + ".chance", 100.0);

            // Parse display-item
            String matName = cfg.getString(rewardPath + ".display-item.material", "CHEST");
            Material mat = parseMaterial(matName, Material.CHEST);
            int dataInt  = cfg.getInt(rewardPath + ".display-item.data", 0);
            short data   = (short) Math.max(0, Math.min(dataInt, Short.MAX_VALUE));
            String name  = MessageUtil.colorize(cfg.getString(rewardPath + ".display-item.name", "&fReward"));
            List<String> lore = MessageUtil.colorizeList(cfg.getStringList(rewardPath + ".display-item.lore"));

            // Parse hidden commands
            List<String> commands = cfg.getStringList(rewardPath + ".commands");

            out.add(new TierReward(chance, mat, data, name, lore, commands));
        }
        return out;
    }

    /** Level required to reach the specified tier (total level across all skills). */
    public int getRequiredLevel(int tier) {
        return tierRequirements.getOrDefault(tier, 0);
    }

    /** All rewards for a given tier (shared across all skills). */
    public List<TierReward> getRewards(int tier) {
        List<TierReward> list = tierRewards.get(tier);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Called after any skill levels up to check whether the player's total
     * level has unlocked any new shared tiers.
     * Sends a notification message but does NOT auto-dispatch rewards.
     */
    public void checkTierUnlock(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int totalLevel = data.getTotalLevel();
        int currentTier = data.getTier();

        for (int tier = currentTier + 1; tier <= TIER_COUNT; tier++) {
            if (totalLevel >= getRequiredLevel(tier)) {
                data.setTier(tier);
                String msg = MessageUtil.getMessage(plugin, "skill.tier-unlock")
                        .replace("{tier}", String.valueOf(tier));
                player.sendMessage(msg);
            } else {
                break;
            }
        }
    }

    /**
     * Dispatch all rewards for a specific tier to a player.
     * Each reward is rolled independently based on its {@code chance}.
     * Marks the tier as claimed and persists the data.
     *
     * @param player the player claiming the reward
     * @param tier   the tier number
     * @return true if rewards were dispatched, false if already claimed or not unlocked
     */
    public boolean dispatchRewards(Player player, int tier) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.hasClaimedTier(tier)) return false;
        if (data.getTier() < tier)      return false;

        data.claimTier(tier);

        List<TierReward> rewards = getRewards(tier);
        for (TierReward reward : rewards) {
            if (reward.rollSuccess()) {
                executeCommands(player, reward);
            }
        }

        plugin.getDataManager().savePlayer(player.getUniqueId());

        String msg = MessageUtil.getMessage(plugin, "rewards.claimed")
                .replace("{tier}", String.valueOf(tier));
        player.sendMessage(msg);

        return true;
    }

    /** Execute all hidden console commands for a single TierReward. */
    private void executeCommands(Player player, TierReward reward) {
        for (String raw : reward.getCommands()) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = raw.replace("%player%", player.getName());
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to dispatch reward command '" + cmd + "': " + e.getMessage());
            }
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown material '" + name + "', using " + fallback.name());
            return fallback;
        }
    }
}
