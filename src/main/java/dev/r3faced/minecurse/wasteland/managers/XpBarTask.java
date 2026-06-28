package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task that updates the XP bar for players in Wasteland worlds.
 * <p>
 * Runs EVERY TICK (1 tick = 50ms) to prevent vanilla XP from
 * interfering with the Wasteland XP bar display.
 * <p>
 * Shows the player's LOWEST skill level on the XP bar, with the
 * bar fill representing progress toward the next level of that skill.
 */
public class XpBarTask extends BukkitRunnable {

    private final WastelandPlugin plugin;

    public XpBarTask(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                updatePlayer(player);
            } catch (Exception ignored) {
                // Fail silently — don't break the task for all players.
            }
        }
    }

    private void updatePlayer(Player player) {
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (!data.isSettingXpBarDisplay()) return;

        // Find the lowest skill.
        SkillType lowestSkill = SkillType.MINING;
        int lowestLevel = Integer.MAX_VALUE;
        for (SkillType skill : SkillType.values()) {
            int lvl = data.getLevel(skill);
            if (lvl < lowestLevel) {
                lowestLevel = lvl;
                lowestSkill = skill;
            }
        }

        int currentLevel = data.getLevel(lowestSkill);
        int cap = plugin.getSkillManager().getLevelCap(lowestSkill);

        // Set the XP bar level to the current Wasteland level.
        player.setLevel(currentLevel);

        // Calculate XP progress within the current level.
        if (currentLevel >= cap) {
            player.setExp(1.0f);
        } else {
            // xpToNextLevel returns the XP needed to go from currentLevel
            // to currentLevel+1 (just the delta, not cumulative).
            long xpToNext = plugin.getSkillManager().xpToNextLevel(lowestSkill, currentLevel);
            if (xpToNext <= 0) {
                player.setExp(0f);
            } else {
                // Total XP accumulated minus total XP needed to reach
                // the current level = XP into this level.
                long xpForCurrentLevel = plugin.getSkillManager()
                        .xpRequiredForLevel(lowestSkill, currentLevel);
                long playerTotalXp = data.getXp(lowestSkill);
                long xpIntoLevel = playerTotalXp - xpForCurrentLevel;
                if (xpIntoLevel < 0) xpIntoLevel = 0;

                float progress = (float) xpIntoLevel / (float) xpToNext;
                if (progress < 0f) progress = 0f;
                if (progress > 0.999f) progress = 0.999f; // Never show 100% (that triggers vanilla level-up)
                player.setExp(progress);
            }
        }
    }

    public void start() {
        try {
            // Run every tick (1 tick) to prevent vanilla XP interference.
            runTaskTimer(plugin, 1L, 1L);
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not schedule XP bar task: " + ex.getMessage());
        }
    }
}
