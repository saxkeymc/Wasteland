package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.api.event.WastelandStoredRewardAddEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandStoredRewardClaimEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandTierChangeEvent;
import dev.r3faced.minecurse.wasteland.api.event.WastelandTierUnlockEvent;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
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

public class TierManager {

    private final WastelandPlugin plugin;

    public static final int TIER_COUNT = 5;

    private final Map<Integer, Integer> tierRequirements = new HashMap<>();

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

            Map<String, Integer> enchants = new HashMap<>();
            if (cfg.isConfigurationSection(rewardPath + ".display-item.enchants")) {
                ConfigurationSection enchSec = cfg.getConfigurationSection(rewardPath + ".display-item.enchants");
                for (String enchName : enchSec.getKeys(false)) {
                    enchants.put(enchName.toUpperCase(), enchSec.getInt(enchName, 1));
                }
            }

            List<String> itemFlags = new ArrayList<>();
            for (String flagName : cfg.getStringList(rewardPath + ".display-item.item-flags")) {
                itemFlags.add(flagName.toUpperCase());
            }

            List<String> commands = cfg.getStringList(rewardPath + ".commands");

            out.add(new TierReward(chance, mat, data, name, lore, enchants, itemFlags, commands));
        }
        return out;
    }

    public int getRequiredLevel(int tier) {
        return tierRequirements.getOrDefault(tier, 0);
    }

    public List<TierReward> getRewards(int tier) {
        List<TierReward> list = tierRewards.get(tier);
        return list != null ? list : new ArrayList<>();
    }

    public boolean meetsRequirements(PlayerData data, int tier) {
        int required = getRequiredLevel(tier);
        for (SkillType skill : SkillType.values()) {
            if (data.getLevel(skill) < required) return false;
        }
        return true;
    }

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

    public void checkTierUnlock(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentTier = data.getTier();

        for (int tier = currentTier + 1; tier <= TIER_COUNT; tier++) {
            if (!plugin.getStartDateManager().isTierAvailable(tier)) {
                break;
            }
            if (meetsRequirements(data, tier)) {
                WastelandTierUnlockEvent unlockEvent = new WastelandTierUnlockEvent(
                        player, data.getTier(), tier, getRequiredLevel(tier));
                Bukkit.getPluginManager().callEvent(unlockEvent);
                if (unlockEvent.isCancelled()) {
                    break;
                }

                int previousTier = data.getTier();
                data.setTier(tier);
                Bukkit.getPluginManager().callEvent(new WastelandTierChangeEvent(
                        player, previousTier, tier, WastelandChangeReason.GAMEPLAY));

                List<TierReward> rewards = getRewards(tier);
                int added = 0;
                for (TierReward reward : rewards) {
                    if (reward.rollSuccess()) {
                        StoredReward stored = new StoredReward(
                                reward.getDisplayMaterial(),
                                reward.getDisplayData(),
                                reward.getDisplayName(),
                                reward.getDisplayLore(),
                                reward.getCommands()
                        );
                        WastelandStoredRewardAddEvent rewardEvent = new WastelandStoredRewardAddEvent(
                                player, stored, tier, WastelandChangeReason.REWARD);
                        Bukkit.getPluginManager().callEvent(rewardEvent);
                        if (rewardEvent.isCancelled()) {
                            continue;
                        }
                        data.addStoredReward(rewardEvent.getReward());
                        added++;
                    }
                }

                plugin.getDataManager().savePlayer(player.getUniqueId());

                String msg = MessageUtil.getMessage(plugin, "skill.tier-unlock")
                        .replace("{tier}", String.valueOf(tier));
                player.sendMessage(msg);

                if (added > 0) {
                    String rewardMsg = MessageUtil.getMessage(plugin, "rewards.added-to-storage")
                            .replace("{count}", String.valueOf(added))
                            .replace("{tier}", String.valueOf(tier));
                    player.sendMessage(rewardMsg);
                }
            } else {
                break;
            }
        }
    }

    public boolean claimStoredReward(Player player, StoredReward reward) {
        if (reward == null) return false;
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.getStoredRewards().contains(reward)) return false;

        WastelandStoredRewardClaimEvent claimEvent = new WastelandStoredRewardClaimEvent(player, reward);
        Bukkit.getPluginManager().callEvent(claimEvent);
        if (claimEvent.isCancelled()) return false;

        for (String raw : reward.getCommands()) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = raw.replace("%player%", player.getName());
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to dispatch reward command '" + cmd + "': " + e.getMessage());
            }
        }

        data.removeStoredReward(reward);
        plugin.getDataManager().savePlayer(player.getUniqueId());

        String msg = MessageUtil.getMessage(plugin, "rewards.claimed-single");
        player.sendMessage(msg);
        return true;
    }

    public void sendUnlockFailedMessage(Player player, int tier) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int required = getRequiredLevel(tier);
        FileConfiguration msgs = plugin.getConfigManager().getMessages();

        String metTemplate     = msgs.getString("tier.requirement-met", "&a\u2714 Level {current}");
        String notMetTemplate  = msgs.getString("tier.requirement-not-met", "&c\u2718 Level {current}/{required}");

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

        List<String> lines = msgs.getStringList("tier.unlock-failed");
        if (lines.isEmpty()) {
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

    public boolean dispatchRewards(Player player, int tier) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.hasClaimedTier(tier)) return false;
        if (data.getTier() < tier) {
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

    public void savePreviewRewards(int tier, List<PreviewRewardEntry> rewards) {
        FileConfiguration cfg = plugin.getConfigManager().getTiers();
        String basePath = "tiers." + tier + ".rewards";

        cfg.set(basePath, null);

        for (int i = 0; i < rewards.size(); i++) {
            PreviewRewardEntry entry = rewards.get(i);
            String rewardPath = basePath + ".reward_" + (i + 1);

            cfg.set(rewardPath + ".chance", entry.getChance());

            cfg.set(rewardPath + ".display-item.material", entry.getMaterial().name());
            cfg.set(rewardPath + ".display-item.data", (int) entry.getData());
            cfg.set(rewardPath + ".display-item.name", entry.getName());
            cfg.set(rewardPath + ".display-item.lore", entry.getLore());

            if (entry.getEnchants() != null && !entry.getEnchants().isEmpty()) {
                for (Map.Entry<String, Integer> ench : entry.getEnchants().entrySet()) {
                    cfg.set(rewardPath + ".display-item.enchants." + ench.getKey(), ench.getValue());
                }
            }

            if (entry.getItemFlags() != null && !entry.getItemFlags().isEmpty()) {
                cfg.set(rewardPath + ".display-item.item-flags", entry.getItemFlags());
            }

            if (entry.getCommands() != null && !entry.getCommands().isEmpty()) {
                cfg.set(rewardPath + ".commands", entry.getCommands());
            }
        }

        try {
            cfg.save(new java.io.File(plugin.getDataFolder(), "tiers.yml"));
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save tiers.yml: " + e.getMessage());
        }

        reload();
    }

    public static class PreviewRewardEntry {
        private final Material material;
        private final short data;
        private final String name;
        private final List<String> lore;
        private final Map<String, Integer> enchants;
        private final List<String> itemFlags;
        private final double chance;
        private final List<String> commands;

        public PreviewRewardEntry(Material material, short data, String name, List<String> lore,
                                   Map<String, Integer> enchants, List<String> itemFlags,
                                   double chance, List<String> commands) {
            this.material = material;
            this.data = data;
            this.name = name;
            this.lore = lore;
            this.enchants = enchants;
            this.itemFlags = itemFlags;
            this.chance = chance;
            this.commands = commands;
        }

        public Material getMaterial() { return material; }
        public short getData() { return data; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public Map<String, Integer> getEnchants() { return enchants; }
        public List<String> getItemFlags() { return itemFlags; }
        public double getChance() { return chance; }
        public List<String> getCommands() { return commands; }
    }
}
