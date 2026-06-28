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
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
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
        if (!cfg.isConfigurationSection("tier-locked-blocks")) return;

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
            // Cancel the break, turn to BEDROCK, give XP + drops, regen after 6s.
            event.setCancelled(true);

            // Award XP.
            String blockType = originalType.name();
            int xp = plugin.getSkillManager().getXpForBlock(skill, blockType);
            if (xp > 0) {
                plugin.getSkillManager().awardXp(player, skill, xp);
            }

            // Give the player the block's natural drops.
            try {
                Collection<ItemStack> drops = block.getDrops(player.getItemInHand());
                if (drops != null && !drops.isEmpty()) {
                    for (ItemStack drop : drops) {
                        if (drop != null && drop.getType() != Material.AIR) {
                            player.getInventory().addItem(drop).values()
                                .forEach(leftover -> player.getWorld().dropItemNaturally(block.getLocation(), leftover));
                        }
                    }
                } else {
                    player.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalType));
                }
            } catch (Exception ignored) {
                player.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalType));
            }

            // Turn the block to BEDROCK.
            block.setType(Material.BEDROCK);

            // Schedule restoration after 6 seconds (120 ticks).
            final Block blockToRestore = block;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (blockToRestore.getType() == Material.BEDROCK) {
                    blockToRestore.setType(originalType);
                }
            }, 120L);
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
}
