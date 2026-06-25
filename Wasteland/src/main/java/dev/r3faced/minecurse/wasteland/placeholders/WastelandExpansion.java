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
 *   %wasteland_mining_level%
 *   %wasteland_mining_xp%
 *   %wasteland_woodcutting_level%
 *   %wasteland_woodcutting_xp%
 *   %wasteland_farming_level%
 *   %wasteland_farming_xp%
 *   %wasteland_fishing_level%
 *   %wasteland_fishing_xp%
 *   %wasteland_total_level%
 *   %wasteland_total_xp%
 *   %wasteland_tier%                — current shared tier
 *   %wasteland_playtime%            — formatted playtime string
 *   %wasteland_playtime_seconds%    — raw playtime in seconds
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

        // Per-skill level/xp placeholders
        for (SkillType skill : SkillType.values()) {
            String key = skill.getKey();
            if (identifier.equals(key + "_level")) {
                return String.valueOf(data.getLevel(skill));
            }
            if (identifier.equals(key + "_xp")) {
                return String.valueOf(data.getXp(skill));
            }
        }

        // Total placeholders
        if (identifier.equals("total_level")) {
            return String.valueOf(data.getTotalLevel());
        }
        if (identifier.equals("total_xp")) {
            return String.valueOf(data.getTotalXp());
        }

        // Shared tier
        if (identifier.equals("tier")) {
            return String.valueOf(data.getTier());
        }

        // Playtime
        if (identifier.equals("playtime")) {
            return PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());
        }
        if (identifier.equals("playtime_seconds")) {
            return String.valueOf(data.getPlaytimeSeconds());
        }

        return null;
    }
}
