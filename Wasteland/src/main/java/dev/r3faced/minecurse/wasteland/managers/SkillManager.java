package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles XP awarding, level-up logic, and tier progression per skill.
 * XP required for the next level: base * (multiplier ^ currentLevel)
 */
public class SkillManager {

    private final WastelandPlugin plugin;

    /** Level caps keyed by skill type. */
    private final Map<SkillType, Integer> levelCaps = new HashMap<>();

    /** XP formula base keyed by skill type. */
    private final Map<SkillType, Double> xpBase = new HashMap<>();

    /** XP formula multiplier keyed by skill type. */
    private final Map<SkillType, Double> xpMultiplier = new HashMap<>();

    /** XP per block/action keyed by skill type, then material name. */
    private final Map<SkillType, Map<String, Integer>> xpValues = new HashMap<>();

    public SkillManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-read skills.yml. */
    public void reload() {
        levelCaps.clear();
        xpBase.clear();
        xpMultiplier.clear();
        xpValues.clear();

        FileConfiguration cfg = plugin.getConfigManager().getSkills();

        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();
            levelCaps.put(skill, cfg.getInt(key + ".level-cap", 50));
            xpBase.put(skill,       cfg.getDouble(key + ".xp-formula.base",       100.0));
            xpMultiplier.put(skill, cfg.getDouble(key + ".xp-formula.multiplier",  1.15));

            Map<String, Integer> blockXp = new HashMap<>();
            if (cfg.isConfigurationSection(key + ".xp")) {
                for (String block : cfg.getConfigurationSection(key + ".xp").getKeys(false)) {
                    blockXp.put(block.toUpperCase(), cfg.getInt(key + ".xp." + block, 0));
                }
            }
            xpValues.put(skill, blockXp);
        }
    }

    // ── Level cap ─────────────────────────────────────────────────────────────

    public int getLevelCap(SkillType skill) {
        return levelCaps.getOrDefault(skill, 50);
    }

    // ── XP per action ─────────────────────────────────────────────────────────

    /**
     * Returns the XP a player should earn for breaking/catching the given material.
     * Falls back to "DEFAULT" key if the specific material is not configured.
     *
     * @param skill    the skill type
     * @param material the material name (e.g. "COAL_ORE")
     * @return XP amount, 0 if not configured
     */
    public int getXpForBlock(SkillType skill, String material) {
        Map<String, Integer> map = xpValues.getOrDefault(skill, new HashMap<>());
        if (map.containsKey(material)) {
            return map.get(material);
        }
        return map.getOrDefault("DEFAULT", 0);
    }

    // ── XP formula ────────────────────────────────────────────────────────────

    /**
     * Calculate total XP required to reach the given level from 0.
     */
    public long xpRequiredForLevel(SkillType skill, int level) {
        if (level <= 0) return 0;
        double base = xpBase.getOrDefault(skill, 100.0);
        double mult = xpMultiplier.getOrDefault(skill, 1.15);
        long total = 0;
        for (int i = 0; i < level; i++) {
            total += (long) (base * Math.pow(mult, i));
        }
        return total;
    }

    /**
     * XP needed to advance from the current level to the next level.
     */
    public long xpToNextLevel(SkillType skill, int currentLevel) {
        double base = xpBase.getOrDefault(skill, 100.0);
        double mult = xpMultiplier.getOrDefault(skill, 1.15);
        return (long) (base * Math.pow(mult, currentLevel));
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    /**
     * Award XP to a player for a specific skill and check for level/tier ups.
     * Must be called on the main thread.
     *
     * @param player the player receiving XP
     * @param skill  the skill to award XP for
     * @param amount the raw XP amount to add
     */
    public void awardXp(Player player, SkillType skill, int amount) {
        if (amount <= 0) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getLevel(skill);
        int cap = getLevelCap(skill);

        if (currentLevel >= cap) return; // already max level

        data.addXp(skill, amount);

        // Notify the player
        String xpMsg = MessageUtil.getMessage(plugin, "skill.xp-gained")
                .replace("{xp}", String.valueOf(amount))
                .replace("{skill}", skill.getKey());
        player.sendMessage(xpMsg);

        // Check for level ups (may be multiple at once with large XP gains)
        boolean levelled = false;
        while (data.getLevel(skill) < cap) {
            long needed = xpRequiredForLevel(skill, data.getLevel(skill) + 1);
            if (data.getXp(skill) >= needed) {
                data.setLevel(skill, data.getLevel(skill) + 1);
                levelled = true;

                String levelMsg = MessageUtil.getMessage(plugin, "skill.level-up")
                        .replace("{skill}", skill.getKey())
                        .replace("{level}", String.valueOf(data.getLevel(skill)));
                player.sendMessage(levelMsg);
            } else {
                break;
            }
        }

        if (levelled) {
            // Check for shared tier unlocks (based on total level across all skills)
            plugin.getTierManager().checkTierUnlock(player);
        }

        // Schedule async save
        plugin.getDataManager().savePlayer(player.getUniqueId());
    }

    /**
     * Returns a progress bar string for the given skill level and XP.
     */
    public String getProgressBar(SkillType skill, PlayerData data) {
        FileConfiguration guiCfg = plugin.getConfigManager().getGui();
        int length = guiCfg.getInt("progress-bar.length", 20);
        String filledChar = guiCfg.getString("progress-bar.filled-char", "■");
        String emptyChar  = guiCfg.getString("progress-bar.empty-char",  "□");
        String filledColor = MessageUtil.colorize(guiCfg.getString("progress-bar.filled-color", "&a"));
        String emptyColor  = MessageUtil.colorize(guiCfg.getString("progress-bar.empty-color",  "&7"));

        int level = data.getLevel(skill);
        int cap   = getLevelCap(skill);

        long currentXp = data.getXp(skill);
        long currentLevelXp = xpRequiredForLevel(skill, level);
        long nextLevelXp    = xpRequiredForLevel(skill, Math.min(level + 1, cap));

        double progress;
        if (level >= cap) {
            progress = 1.0;
        } else {
            long xpInThisLevel  = currentXp - currentLevelXp;
            long xpForThisLevel = nextLevelXp - currentLevelXp;
            progress = xpForThisLevel > 0 ? (double) xpInThisLevel / xpForThisLevel : 1.0;
        }

        int filled = (int) Math.round(progress * length);
        filled = Math.max(0, Math.min(filled, length));

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append(filledColor).append(filledChar);
        for (int i = filled; i < length; i++) bar.append(emptyColor).append(emptyChar);
        return bar.toString();
    }
}
