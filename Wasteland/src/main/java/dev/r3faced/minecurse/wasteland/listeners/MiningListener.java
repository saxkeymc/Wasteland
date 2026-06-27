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
 *       shared tier before they can be broken. If the player's tier is too
 *       low, the break is cancelled and a configurable message is sent.
 *       This check runs at HIGHEST priority (before MONITOR) so it can
 *       cancel the event before XP is awarded.</li>
 *   <li><strong>XP awarding</strong> — when the player holds the Mining Omni
 *       Tool and breaks a block that yields XP, XP is awarded via the
 *       SkillManager.</li>
 * </ol>
 * <p>
 * The tier lock is configurable in config.yml under {@code tier-locked-ores}.
 * Each entry maps a Bukkit Material name to the minimum shared tier required.
 */
public class MiningListener implements Listener {

    private final WastelandPlugin plugin;

    /** Cached ore → required-tier map, loaded from config on enable/reload. */
    private final Map<Material, Integer> tierLockedOres = new HashMap<>();

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
        reloadTierLockedOres();
    }

    /** (Re)load the tier-locked-ores map from config.yml. */
    public void reloadTierLockedOres() {
        tierLockedOres.clear();
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (cfg.isConfigurationSection("tier-locked-ores")) {
            for (String key : cfg.getConfigurationSection("tier-locked-ores").getKeys(false)) {
                int requiredTier = cfg.getInt("tier-locked-ores." + key, 0);
                if (requiredTier <= 0) continue; // disabled
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
     * Tier-lock check — runs at HIGHEST priority so it can cancel the break
     * before the MONITOR handler awards XP.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Only applies in the mining world.
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        // Is this ore tier-locked?
        Integer requiredTier = tierLockedOres.get(blockType);
        if (requiredTier == null) return; // not a locked ore

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int playerTier = data.getTier();

        if (playerTier < requiredTier) {
            // Cancel the break and send a configurable message.
            event.setCancelled(true);
            String oreName = prettifyMaterialName(blockType);
            String msg = MessageUtil.getMessage(plugin, "mining.tier-locked")
                    .replace("{ore}",         oreName)
                    .replace("{tier}",        String.valueOf(requiredTier))
                    .replace("{player_tier}", String.valueOf(playerTier));
            player.sendMessage(msg);
        }
    }

    /**
     * XP awarding — runs at MONITOR priority, only if the break was not
     * cancelled (e.g. by the tier-lock check above).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Verify the player is in a wasteland mining world
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        // Verify the player holds the Mining Omni Tool
        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.MINING)) return;

        // Get XP for this block type
        String blockType = event.getBlock().getType().name();
        int xp = plugin.getSkillManager().getXpForBlock(SkillType.MINING, blockType);
        if (xp <= 0) return;

        plugin.getSkillManager().awardXp(player, SkillType.MINING, xp);
    }

    /** Convert "DIAMOND_ORE" → "Diamond Ore" for display in messages. */
    private String prettifyMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        // Capitalize first letter of each word.
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
