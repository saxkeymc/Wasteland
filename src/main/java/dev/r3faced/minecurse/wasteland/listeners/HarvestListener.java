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

/**
 * Awards Farming XP when the player harvests crops with the Farming Omni Tool.
 * Also delegates tier-locked block checks to the TierLockManager.
 */
public class HarvestListener implements Listener {

    private final WastelandPlugin plugin;

    public HarvestListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Tier-lock check — runs at HIGHEST priority.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTierLockedBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FARMING) return;

        plugin.getTierLockManager().handleBlockBreak(event, SkillType.FARMING);
    }

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

        int xp = plugin.getSkillManager().getXpForBlock(SkillType.FARMING, blockType);
        if (xp <= 0) return;

        plugin.getSkillManager().awardXp(player, SkillType.FARMING, xp, WastelandXpCause.BLOCK_BREAK, blockType);
    }
}
