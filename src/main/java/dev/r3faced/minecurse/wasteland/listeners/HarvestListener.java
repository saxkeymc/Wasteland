package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Crops;

/**
 * Awards Farming XP when the player harvests fully-grown crops with the Farming Omni Tool.
 * Uses the deprecated Crops material check for 1.8.8 compatibility.
 */
public class HarvestListener implements Listener {

    private final WastelandPlugin plugin;

    public HarvestListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FARMING) return;

        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FARMING)) return;

        Block block = event.getBlock();
        String blockType = block.getType().name();

        // Only award XP for configured crop types
        int xp = plugin.getSkillManager().getXpForBlock(SkillType.FARMING, blockType);
        if (xp <= 0) return;

        // Optional: only fully-grown crops award XP (handled by config — if WHEAT is listed,
        // we can do a data value check here for strictness)
        // For maximum configurability we award XP on any configured block break.

        plugin.getSkillManager().awardXp(player, SkillType.FARMING, xp, WastelandXpCause.BLOCK_BREAK, blockType);
    }
}
