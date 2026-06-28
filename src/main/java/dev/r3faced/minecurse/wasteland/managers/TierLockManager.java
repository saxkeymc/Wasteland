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
            // Cancel the break, turn to BEDROCK, give XP only (NO drops),
            // regen after 6s. Blocks are only for getting XP — no item drops.
            event.setCancelled(true);

            // Award XP only — no drops.
            String blockType = originalType.name();
            int xp = plugin.getSkillManager().getXpForBlock(skill, blockType);
            if (xp > 0) {
                plugin.getSkillManager().awardXp(player, skill, xp);
            }

            // Send a FAKE bedrock block change ONLY to the player who broke it.
            // Other players still see the original ore and can mine it.
            player.sendBlockChange(block.getLocation(), Material.BEDROCK, (byte) 0);

            // Schedule restoration after 6 seconds — send the original ore
            // block change back to just this player.
            final Block blockToRestore = block;
            final Player playerToRestore = player;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Only restore if the actual world block is still the original
                // (it might have been changed by another mechanic).
                if (blockToRestore.getType() == originalType) {
                    playerToRestore.sendBlockChange(blockToRestore.getLocation(), originalType, (byte) 0);
                }
            }, 120L); // 6 seconds
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
