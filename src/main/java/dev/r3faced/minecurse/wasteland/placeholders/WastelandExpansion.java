package dev.r3faced.minecurse.wasteland.placeholders;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

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

        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();

            if (identifier.equals(key + "_level")) {
                return String.valueOf(data.getLevel(skill));
            }
            if (identifier.equals(key + "_xp")) {
                return String.valueOf(data.getXp(skill));
            }
            if (identifier.equals(key + "_xp_current")) {
                long totalXp = data.getXp(skill);
                long currentLevelXp = plugin.getSkillManager().xpRequiredForLevel(skill, data.getLevel(skill));
                return String.valueOf(totalXp - currentLevelXp);
            }
            if (identifier.equals(key + "_xp_needed")) {
                long needed = plugin.getSkillManager().xpToNextLevel(skill, data.getLevel(skill));
                return String.valueOf(needed);
            }
            if (identifier.equals(key + "_xp_needed_total")) {
                int nextLevel = data.getLevel(skill) + 1;
                return String.valueOf(plugin.getSkillManager().xpRequiredForLevel(skill, nextLevel));
            }
            if (identifier.equals(key + "_xp_progress_percent")) {
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

        if (identifier.equals("total_level")) {
            return String.valueOf(data.getTotalLevel());
        }
        if (identifier.equals("total_xp")) {
            return String.valueOf(data.getTotalXp());
        }

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
            int lowestSkillLevel = Integer.MAX_VALUE;
            for (SkillType skill : SkillType.values()) {
                int lvl = data.getLevel(skill);
                if (lvl < lowestSkillLevel) lowestSkillLevel = lvl;
            }
            int remaining = required - lowestSkillLevel;
            if (remaining < 0) remaining = 0;
            return String.valueOf(remaining);
        }

        if (identifier.equals("playtime")) {
            return PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());
        }
        if (identifier.equals("playtime_seconds")) {
            return String.valueOf(data.getPlaytimeSeconds());
        }

        if (identifier.equals("dust")) {
            return String.valueOf(data.getDust());
        }

        for (SkillType skill : SkillType.values()) {
            if (identifier.equals(skill.getKey() + "_tool_upgrade")) {
                return String.valueOf(data.getToolUpgradeLevel(skill));
            }
        }

        return null;
    }
}
