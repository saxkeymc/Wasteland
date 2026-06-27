package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Listens for block break events in the wasteland_mining world.
 * <p>
 * Two responsibilities:
 * <ol>
 *   <li><strong>Tier-locked ores</strong> — certain ores (Coal, Iron, Gold,
 *       Emerald, Diamond) require the player to have reached a specific
 *       shared tier before they can be broken.
 *       <ul>
 *         <li>If the player HAS the tier: the break is cancelled, the block
 *             turns to BEDROCK, the player gets the XP and drops manually,
 *             and the block regenerates after 6 seconds.</li>
 *         <li>If the player DOESN'T have the tier: the break is cancelled,
 *             a message is sent, but the block is NOT turned to bedrock —
 *             it stays as the original ore so the player can try again
 *             once they reach the required tier. No XP, no drops.</li>
 *       </ul>
 *   </li>
 *   <li><strong>XP awarding</strong> — when the player holds the Mining Omni
 *       Tool and breaks a non-locked block that yields XP, XP is awarded
 *       via the SkillManager.</li>
 * </ol>
 */
public class MiningListener implements Listener {

    private final WastelandPlugin plugin;
    private final Map<Material, Integer> tierLockedOres = new HashMap<>();

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
        reloadTierLockedOres();
    }

    public void reloadTierLockedOres() {
        tierLockedOres.clear();
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (cfg.isConfigurationSection("tier-locked-ores")) {
            for (String key : cfg.getConfigurationSection("tier-locked-ores").getKeys(false)) {
                int requiredTier = cfg.getInt("tier-locked-ores." + key, 0);
                if (requiredTier <= 0) continue;
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    tierLockedOres.put(mat, requiredTier);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown material '" + key + "' in tier-locked-ores config; skipping.");
                }
            }
        }
    }

    /**
     * Tier-lock + ore-regeneration check — runs at HIGHEST priority.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Only applies in the mining world.
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        Block block = event.getBlock();
        final Material originalType = block.getType();

        // Is this ore tier-locked?
        Integer requiredTier = tierLockedOres.get(originalType);
        if (requiredTier == null) return; // not a locked ore — let normal break proceed

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int playerTier = data.getTier();

        if (playerTier < requiredTier) {
            // ── Player DOESN'T have the tier ───────────────────────────────
            // Cancel the break, send message, but do NOT turn to bedrock.
            // The block stays as the original ore so the player can try
            // again once they reach the required tier.
            event.setCancelled(true);
            String oreName = prettifyMaterialName(originalType);
            String msg = MessageUtil.getMessage(plugin, "mining.tier-locked")
                    .replace("{ore}",         oreName)
                    .replace("{tier}",        String.valueOf(requiredTier))
                    .replace("{player_tier}", String.valueOf(playerTier));
            player.sendMessage(msg);
        } else {
            // ── Player HAS the tier ────────────────────────────────────────
            // Cancel the break (so the block doesn't disappear normally),
            // turn the block to BEDROCK, give the player XP + drops manually,
            // and schedule regeneration after 6 seconds.
            event.setCancelled(true);

            // Award XP (the MONITOR handler won't fire because we cancelled).
            String blockType = originalType.name();
            int xp = plugin.getSkillManager().getXpForBlock(SkillType.MINING, blockType);
            if (xp > 0) {
                plugin.getSkillManager().awardXp(player, SkillType.MINING, xp);
            }

            // Give the player the ore drop manually.
            // For most ores in 1.8, the drop is the ore item itself.
            // We use what the block would naturally drop.
            try {
                // Use the block's natural drops.
                java.util.Collection<ItemStack> drops = block.getDrops(player.getItemInHand());
                if (drops != null && !drops.isEmpty()) {
                    for (ItemStack drop : drops) {
                        if (drop != null && drop.getType() != Material.AIR) {
                            player.getInventory().addItem(drop).values()
                                .forEach(leftover -> player.getWorld().dropItemNaturally(block.getLocation(), leftover));
                        }
                    }
                } else {
                    // Fallback: drop the ore block item itself.
                    player.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalType));
                }
            } catch (Exception ignored) {
                // If drop logic fails, just drop the ore item.
                player.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(originalType));
            }

            // Turn the block to BEDROCK so it can't be broken again while
            // the cooldown is active.
            block.setType(Material.BEDROCK);

            // Schedule restoration after 6 seconds (120 ticks).
            final Block blockToRestore = block;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (blockToRestore.getType() == Material.BEDROCK) {
                    blockToRestore.setType(originalType);
                }
            }, 120L); // 6 seconds
        }
    }

    /**
     * XP awarding for non-locked blocks — runs at MONITOR priority.
     * Only fires if the break was NOT cancelled (e.g. by the tier-lock check).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.MINING)) return;

        String blockType = event.getBlock().getType().name();
        int xp = plugin.getSkillManager().getXpForBlock(SkillType.MINING, blockType);
        if (xp <= 0) return;

        plugin.getSkillManager().awardXp(player, SkillType.MINING, xp);
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
}
