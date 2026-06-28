package dev.r3faced.minecurse.wasteland.placeholders;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion for Wasteland.
 *
 * Available placeholders:
 *
 *   Per-skill:
 *     %wasteland_mining_level%                 — current Mining level
 *     %wasteland_mining_xp%                    — total Mining XP
 *     %wasteland_mining_xp_current%            — XP into current level
 *     %wasteland_mining_xp_needed%             — XP needed for next level (from current level)
 *     %wasteland_mining_xp_needed_total%       — total XP needed to reach next level
 *     %wasteland_mining_xp_progress_percent%   — progress to next level (0-100)
 *     %wasteland_mining_level_cap%             — max level for this skill
 *
 *   Same pattern for woodcutting, farming, fishing.
 *
 *   Totals:
 *     %wasteland_total_level%                  — sum of all 4 skill levels
 *     %wasteland_total_xp%                     — sum of all 4 skill XP
 *
 *   Tier:
 *     %wasteland_tier%                         — current shared tier
 *     %wasteland_next_tier%                    — next tier number (or "MAX" if at cap)
 *     %wasteland_next_tier_level%              — total level required for next tier
 *     %wasteland_next_tier_level_remaining%    — total levels still needed for next tier
 *
 *   Playtime:
 *     %wasteland_playtime%                     — formatted playtime string
 *     %wasteland_playtime_seconds%             — raw playtime in seconds
 */
public class WastelandExpansion extends PlaceholderExpansion {

    private final WastelandPlugin plugin;

    public WastelandExpansion(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "wasteland";
    }

    @Override
    public String getAuthor() {
        return "r3faced";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // ── Per-skill placeholders ──────────────────────────────────────────
        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();

            if (identifier.equals(key + "_level")) {
                return String.valueOf(data.getLevel(skill));
            }
            if (identifier.equals(key + "_xp")) {
                return String.valueOf(data.getXp(skill));
            }
            if (identifier.equals(key + "_xp_current")) {
                // XP into the current level = total XP - XP needed to reach current level
                long totalXp = data.getXp(skill);
                long currentLevelXp = plugin.getSkillManager().xpRequiredForLevel(skill, data.getLevel(skill));
                return String.valueOf(totalXp - currentLevelXp);
            }
            if (identifier.equals(key + "_xp_needed")) {
                // XP needed to go from current level to next level
                long needed = plugin.getSkillManager().xpToNextLevel(skill, data.getLevel(skill));
                return String.valueOf(needed);
            }
            if (identifier.equals(key + "_xp_needed_total")) {
                // Total XP needed to reach the next level (cumulative)
                int nextLevel = data.getLevel(skill) + 1;
                return String.valueOf(plugin.getSkillManager().xpRequiredForLevel(skill, nextLevel));
            }
            if (identifier.equals(key + "_xp_progress_percent")) {
                // Progress percentage to next level (0-100)
                int level = data.getLevel(skill);
                int cap = plugin.getSkillManager().getLevelCap(skill);
                if (level >= cap) return "100";
                long currentLevelXp = plugin.getSkillManager().xpRequiredForLevel(skill, level);
                long nextLevelXp = plugin.getSkillManager().xpRequiredForLevel(skill, level + 1);
                long xpThisLevel = nextLevelXp - currentLevelXp;
                if (xpThisLevel <= 0) return "0";
                long xpIntoLevel = data.getXp(skill) - currentLevelXp;
                long pct = (xpIntoLevel * 100) / xpThisLevel;
                if (pct < 0) pct = 0;
                if (pct > 100) pct = 100;
                return String.valueOf(pct);
            }
            if (identifier.equals(key + "_level_cap")) {
                return String.valueOf(plugin.getSkillManager().getLevelCap(skill));
            }
        }

        // ── Total placeholders ──────────────────────────────────────────────
        if (identifier.equals("total_level")) {
            return String.valueOf(data.getTotalLevel());
        }
        if (identifier.equals("total_xp")) {
            return String.valueOf(data.getTotalXp());
        }

        // ── Tier placeholders ───────────────────────────────────────────────
        if (identifier.equals("tier")) {
            return String.valueOf(data.getTier());
        }
        if (identifier.equals("next_tier")) {
            int currentTier = data.getTier();
            int maxTiers = dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT;
            if (currentTier >= maxTiers) return "MAX";
            return String.valueOf(currentTier + 1);
        }
        if (identifier.equals("next_tier_level")) {
            int currentTier = data.getTier();
            int maxTiers = dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT;
            if (currentTier >= maxTiers) return "0";
            int nextTier = currentTier + 1;
            return String.valueOf(plugin.getTierManager().getRequiredLevel(nextTier));
        }
        if (identifier.equals("next_tier_level_remaining")) {
            int currentTier = data.getTier();
            int maxTiers = dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT;
            if (currentTier >= maxTiers) return "0";
            int nextTier = currentTier + 1;
            int required = plugin.getTierManager().getRequiredLevel(nextTier);
            // The required level must be reached by EVERY skill, so the
            // "remaining" is how many more levels the player's LOWEST skill
            // needs to reach the requirement.
            int lowestSkillLevel = Integer.MAX_VALUE;
            for (SkillType skill : SkillType.values()) {
                int lvl = data.getLevel(skill);
                if (lvl < lowestSkillLevel) lowestSkillLevel = lvl;
            }
            int remaining = required - lowestSkillLevel;
            if (remaining < 0) remaining = 0;
            return String.valueOf(remaining);
        }

        // ── Playtime placeholders ───────────────────────────────────────────
        if (identifier.equals("playtime")) {
            return PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());
        }
        if (identifier.equals("playtime_seconds")) {
            return String.valueOf(data.getPlaytimeSeconds());
        }

        // ── Dust placeholder ────────────────────────────────────────────────
        if (identifier.equals("dust")) {
            return String.valueOf(data.getDust());
        }

        // ── Tool upgrade placeholders ───────────────────────────────────────
        for (SkillType skill : SkillType.values()) {
            if (identifier.equals(skill.getKey() + "_tool_upgrade")) {
                return String.valueOf(data.getToolUpgradeLevel(skill));
            }
        }

        return null;
    }
}
