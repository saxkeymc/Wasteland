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

/**
 * Handles tier-locked block checks for ALL skills (mining, woodcutting,
 * farming, fishing).
 * <p>
 * When a player breaks a tier-locked block:
 * <ul>
 *   <li>If the player HAS the tier: the break is cancelled, the block turns
 *       to BEDROCK, the player gets XP + drops manually, and the block
 *       regenerates after 6 seconds.</li>
 *   <li>If the player DOESN'T have the tier: the break is cancelled, a
 *       message is sent, but the block stays as the original block (NOT
 *       turned to bedrock). No XP, no drops.</li>
 * </ul>
 * <p>
 * The tier-locked blocks are configured per-skill in config.yml under
 * {@code tier-locked-blocks.<skill>}.
 */
public class TierLockManager {

    private final WastelandPlugin plugin;
    /** Map: skill → (material → required tier). */
    private final Map<SkillType, Map<Material, Integer>> lockedBlocks = new HashMap<>();

    public TierLockManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** (Re)load the tier-locked-blocks map from config.yml. */
    public void reload() {
        lockedBlocks.clear();
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();

        // ── New format: tier-locked-blocks.<skill>.<MATERIAL> = tier ──
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

        // ── Old format fallback: tier-locked-ores.<MATERIAL> = tier ──
        // For backwards compatibility with old config.yml files.
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

        // ── Auto-add default ores if mining map is empty ──
        // Ensures coal/iron/gold/emerald/diamond are always tier-locked
        // even if config is missing or misconfigured.
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

        // Log loaded blocks for debugging.
        for (Map.Entry<SkillType, Map<Material, Integer>> entry : lockedBlocks.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                plugin.getLogger().info("TierLockManager: " + entry.getKey().getKey() +
                        " has " + entry.getValue().size() + " tier-locked blocks.");
            }
        }
    }

    /**
     * Check if a block break should be tier-locked. Returns true if the
     * event was handled (caller should return immediately), false if the
     * break should proceed normally.
     * <p>
     * This method handles:
     * <ul>
     *   <li>Cancelling the event</li>
     *   <li>Sending the tier-locked message (if player doesn't have tier)</li>
     *   <li>Turning the block to bedrock + giving XP/drops + scheduling
     *       regeneration (if player has tier)</li>
     * </ul>
     *
     * @param event the BlockBreakEvent
     * @param skill the skill for the current world
     * @return true if handled (event cancelled), false if break should proceed
     */
    public boolean handleBlockBreak(BlockBreakEvent event, SkillType skill) {
        if (skill == null) return false;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material originalType = block.getType();

        // Check if this block is on fake-bedrock cooldown for this player.
        // If yes, cancel — the player can't break it again until the 6s
        // cooldown ends.
        if (plugin.getFakeBlockManager().isFakeBedrock(player, block.getLocation())) {
            event.setCancelled(true);
            return true;
        }

        Map<Material, Integer> skillMap = lockedBlocks.get(skill);
        if (skillMap == null) return false;

        Integer requiredTier = skillMap.get(originalType);
        if (requiredTier == null) return false; // not a locked block

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int playerTier = data.getTier();

        if (playerTier < requiredTier) {
            // ── Player DOESN'T have the tier ───────────────────────────────
            // Cancel the break, send message, do NOT turn to bedrock.
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
            // ── Player HAS the tier ────────────────────────────────────────
            // Cancel the break, turn to BEDROCK (fake, per-player), give XP +
            // dust, regen after 6s. Blocks are only for getting XP — no drops.
            event.setCancelled(true);

            // Award XP.
            String blockType = originalType.name();
            int xp = plugin.getSkillManager().getXpForBlock(skill, blockType);
            if (xp > 0) {
                plugin.getSkillManager().awardXp(player, skill, xp);
            }

            // Award Dust (was missing — MONITOR handler never fires because
            // the event is cancelled here at HIGHEST).
            plugin.getDustManager().awardDust(player,
                    plugin.getDustManager().getDefaultDustPerAction(skill));

            // Random money drop (mining only).
            if (skill == SkillType.MINING) {
                tryRollMoneyDrop(player);
            }

            // Send a FAKE bedrock block change ONLY to the player who broke it.
            // DELAYED by 1 tick to avoid being overwritten by the server's
            // block-update packet on event cancellation.
            final Block blockToRestore = block;
            final Player playerToRestore = player;
            final Material origType = originalType;
            final org.bukkit.Location blockLoc = block.getLocation();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playerToRestore.sendBlockChange(blockLoc, Material.BEDROCK, (byte) 0);
                // Register with FakeBlockManager so interactions re-send bedrock.
                plugin.getFakeBlockManager().addFakeBlock(playerToRestore, blockLoc, origType);
            }, 1L);

            // Schedule restoration after 6 seconds.
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (blockToRestore.getType() == origType) {
                    playerToRestore.sendBlockChange(blockLoc, origType, (byte) 0);
                }
                plugin.getFakeBlockManager().removeFakeBlock(playerToRestore, blockLoc);
            }, 121L);
            return true;
        }
    }

    /** Convert "DIAMOND_ORE" → "Diamond Ore" for display in messages. */
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

    /** Random money drop — same logic as MiningListener. */
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
