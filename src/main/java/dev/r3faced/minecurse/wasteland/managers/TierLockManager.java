package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;

public class TierLockManager {

    private final WastelandPlugin plugin;
    private final Map<SkillType, Map<Material, Integer>> lockedBlocks = new HashMap<>();

    public TierLockManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        lockedBlocks.clear();
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();

        if (cfg.isConfigurationSection("tier-locked-blocks")) {
            for (SkillType skill : SkillType.values()) {
                Map<Material, Integer> skillMap = new HashMap<>();
                String basePath = "tier-locked-blocks." + skill.getKey();
                if (cfg.isConfigurationSection(basePath)) {
                    for (String matName : cfg.getConfigurationSection(basePath).getKeys(false)) {
                        int requiredTier = cfg.getInt(basePath + "." + matName, 0);
                        if (requiredTier <= 0) continue;
                        try {
                            Material mat = Material.valueOf(matName.toUpperCase());
                            skillMap.put(mat, requiredTier);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Unknown material '" + matName +
                                    "' in tier-locked-blocks." + skill.getKey() + "; skipping.");
                        }
                    }
                }
                lockedBlocks.put(skill, skillMap);
            }
        }

        if (cfg.isConfigurationSection("tier-locked-ores")) {
            Map<Material, Integer> miningMap = lockedBlocks.getOrDefault(SkillType.MINING, new HashMap<>());
            for (String matName : cfg.getConfigurationSection("tier-locked-ores").getKeys(false)) {
                int requiredTier = cfg.getInt("tier-locked-ores." + matName, 0);
                if (requiredTier <= 0) continue;
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    if (!miningMap.containsKey(mat)) {
                        miningMap.put(mat, requiredTier);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
            lockedBlocks.put(SkillType.MINING, miningMap);
        }

        Map<Material, Integer> miningMap = lockedBlocks.getOrDefault(SkillType.MINING, new HashMap<>());
        if (miningMap.isEmpty()) {
            miningMap.put(Material.COAL_ORE, 1);
            miningMap.put(Material.IRON_ORE, 2);
            miningMap.put(Material.GOLD_ORE, 3);
            miningMap.put(Material.EMERALD_ORE, 4);
            miningMap.put(Material.DIAMOND_ORE, 5);
            lockedBlocks.put(SkillType.MINING, miningMap);
            plugin.getLogger().info("TierLockManager: Auto-added default tier-locked ores (config was empty).");
        }

        for (Map.Entry<SkillType, Map<Material, Integer>> entry : lockedBlocks.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                plugin.getLogger().info("TierLockManager: " + entry.getKey().getKey() +
                        " has " + entry.getValue().size() + " tier-locked blocks.");
            }
        }
    }

    public boolean handleBlockBreak(BlockBreakEvent event, SkillType skill) {
        if (skill == null) return false;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material originalType = block.getType();

        if (plugin.getFakeBlockManager().isFakeBedrock(player, block.getLocation())) {
            event.setCancelled(true);
            return true;
        }

        Map<Material, Integer> skillMap = lockedBlocks.get(skill);
        if (skillMap == null) return false;

        Integer requiredTier = skillMap.get(originalType);
        if (requiredTier == null) return false;

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int playerTier = data.getTier();

        if (playerTier < requiredTier) {
            event.setCancelled(true);
            String blockName = prettifyMaterialName(originalType);
            String msg = MessageUtil.getMessage(plugin, "block.tier-locked")
                    .replace("{block}",      blockName)
                    .replace("{tier}",       String.valueOf(requiredTier))
                    .replace("{player_tier}", String.valueOf(playerTier))
                    .replace("{skill}",      skill.getKey());
            player.sendMessage(msg);
            return true;
        } else {
            event.setCancelled(true);

            String blockType = originalType.name();
            int xp = plugin.getSkillManager().getXpForBlock(skill, blockType);
            if (xp > 0) {
                plugin.getSkillManager().awardXp(player, skill, xp);
            }

            plugin.getDustManager().awardDust(player,
                    plugin.getDustManager().getDefaultDustPerAction(skill));

            if (skill == SkillType.MINING) {
                tryRollMoneyDrop(player);
            }

            final Block blockToRestore = block;
            final Player playerToRestore = player;
            final Material origType = originalType;
            final org.bukkit.Location blockLoc = block.getLocation();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playerToRestore.sendBlockChange(blockLoc, Material.BEDROCK, (byte) 0);
                plugin.getFakeBlockManager().addFakeBlock(playerToRestore, blockLoc, origType);
            }, 1L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (blockToRestore.getType() == origType) {
                    playerToRestore.sendBlockChange(blockLoc, origType, (byte) 0);
                }
                plugin.getFakeBlockManager().removeFakeBlock(playerToRestore, blockLoc);
            }, 121L);
            return true;
        }
    }

    private String prettifyMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean capitalize = true;
        for (char c : name.toCharArray()) {
            if (capitalize && c >= 'a' && c <= 'z') {
                out.append((char) (c - 32));
            } else {
                out.append(c);
            }
            capitalize = (c == ' ');
        }
        return out.toString();
    }

    private void tryRollMoneyDrop(Player player) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (!cfg.getBoolean("mining-money-drops.enabled", true)) return;

        double chance = cfg.getDouble("mining-money-drops.chance", 5.0);
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        int min = cfg.getInt("mining-money-drops.min-amount", 5000);
        int max = cfg.getInt("mining-money-drops.max-amount", 7000);
        int amount = java.util.concurrent.ThreadLocalRandom.current().nextInt(min, max + 1);

        String command = cfg.getString("mining-money-drops.command", "eco give %player% {amount}");
        command = command.replace("%player%", player.getName()).replace("{amount}", String.valueOf(amount));

        try {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to dispatch money-drop command: " + e.getMessage());
            return;
        }

        String prefix = dev.r3faced.minecurse.wasteland.utils.MessageUtil
                .colorize(plugin.getConfigManager().getMessages().getString("prefix", ""));
        String msg = dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                cfg.getString("mining-money-drops.message",
                        "{prefix}&aYou found &2${amount} &awhile mining!")
                        .replace("{prefix}", prefix)
                        .replace("{amount}", String.valueOf(amount)));
        player.sendMessage(msg);
    }
}
