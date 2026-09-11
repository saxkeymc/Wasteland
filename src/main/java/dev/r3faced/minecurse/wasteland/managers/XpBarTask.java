package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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
            } catch (Exception ignored) {}
        }
    }

    private void updatePlayer(Player player) {
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (!data.isSettingXpBarDisplay()) return;

        SkillType skill = data.getActiveSkill();
        if (skill == null) {
            ItemStack hand = player.getItemInHand();
            for (SkillType s : SkillType.values()) {
                if (plugin.getToolManager().isOmniTool(hand, s)) {
                    skill = s;
                    data.setActiveSkill(s);
                    break;
                }
            }
            if (skill == null) {
                skill = SkillType.MINING;
            }
        }

        int currentLevel = data.getLevel(skill);
        int cap = plugin.getSkillManager().getLevelCap(skill);

        player.setLevel(currentLevel);

        if (currentLevel >= cap) {
            player.setExp(1.0f);
        } else {
            long xpToNext = plugin.getSkillManager().xpToNextLevel(skill, currentLevel);
            if (xpToNext <= 0) {
                player.setExp(0f);
            } else {
                long xpForCurrentLevel = plugin.getSkillManager().xpRequiredForLevel(skill, currentLevel);
                long playerTotalXp = data.getXp(skill);
                long xpIntoLevel = playerTotalXp - xpForCurrentLevel;
                if (xpIntoLevel < 0) xpIntoLevel = 0;

                float progress = (float) xpIntoLevel / (float) xpToNext;
                if (progress < 0f) progress = 0f;
                if (progress > 0.999f) progress = 0.999f;
                player.setExp(progress);
            }
        }
    }

    public void start() {
        try {
            runTaskTimer(plugin, 1L, 1L);
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("Could not schedule XP bar task: " + ex.getMessage());
        }
    }
}
