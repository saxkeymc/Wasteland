package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
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
 * There is only ONE tier progression per player. To unlock the next tier,
 * <strong>EVERY skill</strong> (Mining, Chopping, Farming, Fishing) must
 * reach that tier's required level. Rushing a single skill will NOT unlock
 * the next tier — players are encouraged to level all four skills equally.
 * <p>
 * Reward config format (tiers.yml):
 * <pre>
 * tiers:
 *   1:
 *     required-level: 10
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
    public static final int TIER_COUNT = 5;

    /** Level required to unlock each tier — must be reached by EVERY skill. */
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
            tierRequirements.put(tier, cfg.getInt(path + ".required-level", tier * 10));
            tierRewards.put(tier, parseRewards(cfg, path + ".rewards"));
        }
    }

    private List<TierReward> parseRewards(FileConfiguration cfg, String basePath) {
        List<TierReward> out = new ArrayList<>();
        if (!cfg.isConfigurationSection(basePath)) return out;

        ConfigurationSection parent = cfg.getConfigurationSection(basePath);
        for (String rewardKey : parent.getKeys(false)) {
            String rewardPath = basePath + "." + rewardKey;

            double chance = cfg.getDouble(rewardPath + ".chance", 100.0);

            String matName = cfg.getString(rewardPath + ".display-item.material", "CHEST");
            Material mat = parseMaterial(matName, Material.CHEST);
            int dataInt  = cfg.getInt(rewardPath + ".display-item.data", 0);
            short data   = (short) Math.max(0, Math.min(dataInt, Short.MAX_VALUE));
            String name  = MessageUtil.colorize(cfg.getString(rewardPath + ".display-item.name", "&fReward"));
            List<String> lore = MessageUtil.colorizeList(cfg.getStringList(rewardPath + ".display-item.lore"));

            List<String> commands = cfg.getStringList(rewardPath + ".commands");

            out.add(new TierReward(chance, mat, data, name, lore, commands));
        }
        return out;
    }

    /** Level required to reach the specified tier (each skill must reach this). */
    public int getRequiredLevel(int tier) {
        return tierRequirements.getOrDefault(tier, 0);
    }

    /** All rewards for a given tier (shared across all skills). */
    public List<TierReward> getRewards(int tier) {
        List<TierReward> list = tierRewards.get(tier);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Returns true if EVERY skill has reached the required level for the
     * given tier.
     */
    public boolean meetsRequirements(PlayerData data, int tier) {
        int required = getRequiredLevel(tier);
        for (SkillType skill : SkillType.values()) {
            if (data.getLevel(skill) < required) return false;
        }
        return true;
    }

    /**
     * Returns the list of skills that have NOT yet reached the required
     * level for the given tier. Empty list if all skills meet the req.
     */
    public List<SkillType> getMissingSkills(PlayerData data, int tier) {
        int required = getRequiredLevel(tier);
        List<SkillType> missing = new ArrayList<>();
        for (SkillType skill : SkillType.values()) {
            if (data.getLevel(skill) < required) {
                missing.add(skill);
            }
        }
        return missing;
    }

    /**
     * Called after any skill levels up to check whether the player's
     * ALL-SKILLS requirement has been met for the next tier.
     * Sends a notification message on unlock. Does NOT auto-dispatch rewards.
     */
    public void checkTierUnlock(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentTier = data.getTier();

        for (int tier = currentTier + 1; tier <= TIER_COUNT; tier++) {
            if (meetsRequirements(data, tier)) {
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
     * Send the player a configurable message listing which skills still
     * need to be leveled up to unlock the given tier.
     */
    public void sendUnlockFailedMessage(Player player, int tier) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int required = getRequiredLevel(tier);
        FileConfiguration msgs = plugin.getConfigManager().getMessages();

        String metTemplate     = msgs.getString("tier.requirement-met", "&a\u2714 Level {current}");
        String notMetTemplate  = msgs.getString("tier.requirement-not-met", "&c\u2718 Level {current}/{required}");

        // Per-skill status strings (e.g. "&a✔ Level 10" or "&c✘ Level 8/10")
        Map<String, String> statuses = new HashMap<>();
        for (SkillType skill : SkillType.values()) {
            int current = data.getLevel(skill);
            String status;
            if (current >= required) {
                status = MessageUtil.colorize(metTemplate
                        .replace("{current}",  String.valueOf(current))
                        .replace("{required}", String.valueOf(required)));
            } else {
                status = MessageUtil.colorize(notMetTemplate
                        .replace("{current}",  String.valueOf(current))
                        .replace("{required}", String.valueOf(required)));
            }
            statuses.put(skill.getKey() + "_status", status);
        }

        // Build the multi-line message
        List<String> lines = msgs.getStringList("tier.unlock-failed");
        if (lines.isEmpty()) {
            // Fallback if config missing
            player.sendMessage(MessageUtil.colorize("&cYou have not unlocked this tier yet!"));
            return;
        }
        for (String raw : lines) {
            String line = raw;
            for (Map.Entry<String, String> e : statuses.entrySet()) {
                line = line.replace("{" + e.getKey() + "}", e.getValue());
            }
            line = line.replace("{tier}",     String.valueOf(tier));
            line = line.replace("{required}", String.valueOf(required));
            player.sendMessage(MessageUtil.colorize(line));
        }
    }

    /**
     * Dispatch all rewards for a specific tier to a player.
     * Each reward is rolled independently based on its {@code chance}.
     * Marks the tier as claimed and persists the data.
     */
    public boolean dispatchRewards(Player player, int tier) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.hasClaimedTier(tier)) return false;
        if (data.getTier() < tier) {
            // Not unlocked yet — send the requirements message.
            sendUnlockFailedMessage(player, tier);
            return false;
        }

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
