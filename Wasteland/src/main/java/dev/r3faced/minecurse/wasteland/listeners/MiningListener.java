package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for block break events in wasteland_mining worlds.
 * Awards Mining XP when the player holds the Mining Omni Tool.
 */
public class MiningListener implements Listener {

    private final WastelandPlugin plugin;

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

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
}
