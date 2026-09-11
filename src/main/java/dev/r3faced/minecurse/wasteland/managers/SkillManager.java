package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.api.event.WastelandSkillChangeEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandSkillLevelUpEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandXpGainEvent;
import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SkillManager {

    private final WastelandPlugin plugin;

    private final Map<SkillType, Integer> levelCaps = new HashMap<>();

    private final Map<SkillType, Double> xpBase = new HashMap<>();

    private final Map<SkillType, Double> xpMultiplier = new HashMap<>();

    private final Map<SkillType, Map<String, Integer>> xpValues = new HashMap<>();

    public SkillManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

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

    public int getLevelCap(SkillType skill) {
        return levelCaps.getOrDefault(skill, 50);
    }

    public int getXpForBlock(SkillType skill, String material) {
        Map<String, Integer> map = xpValues.getOrDefault(skill, new HashMap<>());
        if (map.containsKey(material)) {
            return map.get(material);
        }
        return map.getOrDefault("DEFAULT", 0);
    }

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

    public long xpToNextLevel(SkillType skill, int currentLevel) {
        double base = xpBase.getOrDefault(skill, 100.0);
        double mult = xpMultiplier.getOrDefault(skill, 1.15);
        return (long) (base * Math.pow(mult, currentLevel));
    }

    public void awardXp(Player player, SkillType skill, int amount) {
        awardXp(player, skill, amount, WastelandXpCause.CUSTOM, "legacy");
    }

    public long awardXp(Player player, SkillType skill, long amount, WastelandXpCause cause, String source) {
        if (amount <= 0) return 0L;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getLevel(skill);
        int cap = getLevelCap(skill);

        if (currentLevel >= cap) return 0L;

        WastelandXpCause safeCause = cause == null ? WastelandXpCause.CUSTOM : cause;
        WastelandXpGainEvent xpEvent = new WastelandXpGainEvent(player, skill, amount, safeCause, source);
        Bukkit.getPluginManager().callEvent(xpEvent);
        if (xpEvent.isCancelled() || xpEvent.getAmount() <= 0L) {
            return 0L;
        }

        long finalAmount = xpEvent.getAmount();
        int oldLevel = data.getLevel(skill);
        long oldXp = data.getXp(skill);

        data.addXp(skill, finalAmount);

        if (data.isSettingXpNoises()) {
            try {
                org.bukkit.Sound xpSound = org.bukkit.Sound.valueOf("ORB_PICKUP");
                player.playSound(player.getLocation(), xpSound, 0.3f, 1.2f);
            } catch (Exception ignored) {
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.3f, 1.5f);
                } catch (Exception ignored2) {}
            }
        }

        String xpMsg = MessageUtil.getMessage(plugin, "skill.xp-gained")
                .replace("{xp}", String.valueOf(finalAmount))
                .replace("{skill}", skill.getKey());
        if (!xpMsg.isEmpty()) {
            player.sendMessage(xpMsg);
        }

        boolean levelled = false;
        while (data.getLevel(skill) < cap) {
            long needed = xpRequiredForLevel(skill, data.getLevel(skill) + 1);
            if (data.getXp(skill) >= needed) {
                int before = data.getLevel(skill);
                data.setLevel(skill, data.getLevel(skill) + 1);
                levelled = true;

                String levelMsg = MessageUtil.getMessage(plugin, "skill.level-up")
                        .replace("{skill}", skill.getKey())
                        .replace("{level}", String.valueOf(data.getLevel(skill)));
                player.sendMessage(levelMsg);

                String skillName = skill.getKey().substring(0, 1).toUpperCase() + skill.getKey().substring(1);
                String title = dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize("&a&lLevel Up!");
                String subtitle = dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                        "&7" + skillName + " &aLevel " + data.getLevel(skill));
                player.sendTitle(title, subtitle);

                Bukkit.getPluginManager().callEvent(new WastelandSkillLevelUpEvent(
                        player, skill, before, data.getLevel(skill), safeCause, source));
            } else {
                break;
            }
        }

        Bukkit.getPluginManager().callEvent(new WastelandSkillChangeEvent(
                player,
                skill,
                oldLevel,
                data.getLevel(skill),
                oldXp,
                data.getXp(skill),
                reasonFromCause(safeCause)
        ));

        if (levelled) {
            plugin.getTierManager().checkTierUnlock(player);
        }

        plugin.getDataManager().savePlayer(player.getUniqueId());
        return finalAmount;
    }

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

    private WastelandChangeReason reasonFromCause(WastelandXpCause cause) {
        switch (cause) {
            case BLOCK_BREAK:
            case FISHING:
                return WastelandChangeReason.GAMEPLAY;
            case COMMAND:
                return WastelandChangeReason.COMMAND;
            case API:
                return WastelandChangeReason.API;
            case CUSTOM:
            default:
                return WastelandChangeReason.CUSTOM;
        }
    }
}
