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
 * The XP bar shows the player's TOTAL level across all 4 skills, with the
 * bar fill representing progress toward the next level.
 * <p>
 * Only applies if the player has the setting enabled. Vanilla XP is
 * saved when entering a Wasteland world and restored when leaving.
 */
public class XpBarTask extends BukkitRunnable {

    private final WastelandPlugin plugin;
    private static final long INTERVAL_TICKS = 10L; // Update every 0.5 seconds

    public XpBarTask(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) continue;

            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data == null) continue;

            // Only update if the setting is enabled.
            if (!data.isSettingXpBarDisplay()) continue;

            // Use the player's lowest skill for the XP bar (since tier
            // unlocks require ALL skills at the same level). This gives
            // a more meaningful "progress" bar.
            SkillType lowestSkill = null;
            int lowestLevel = Integer.MAX_VALUE;
            for (SkillType skill : SkillType.values()) {
                int lvl = data.getLevel(skill);
                if (lvl < lowestLevel) {
                    lowestLevel = lvl;
                    lowestSkill = skill;
                }
            }

            if (lowestSkill == null) continue;

            int currentLevel = data.getLevel(lowestSkill);
            int cap = plugin.getSkillManager().getLevelCap(lowestSkill);

            // Set the XP bar level to the current Wasteland level.
            player.setLevel(currentLevel);

            // Calculate XP progress within the current level.
            if (currentLevel >= cap) {
                player.setExp(1.0f); // Maxed out
            } else {
                long currentLevelXp = plugin.getSkillManager().xpRequiredForLevel(lowestSkill, currentLevel);
                long nextLevelXp = plugin.getSkillManager().xpRequiredForLevel(lowestSkill, currentLevel + 1);
                long xpThisLevel = nextLevelXp - currentLevelXp;
                if (xpThisLevel <= 0) {
                    player.setExp(0f);
                } else {
                    long xpIntoLevel = data.getXp(lowestSkill) - currentLevelXp;
                    float progress = (float) xpIntoLevel / (float) xpThisLevel;
                    if (progress < 0f) progress = 0f;
                    if (progress > 1f) progress = 1f;
                    player.setExp(progress);
                }
            }
        }
    }

    public void start() {
        try {
            runTaskTimer(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not schedule XP bar task: " + ex.getMessage());
        }
    }
}
