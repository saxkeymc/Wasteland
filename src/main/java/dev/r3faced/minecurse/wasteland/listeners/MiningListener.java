package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for block break events in the mining world.
 * <p>
 * Two responsibilities:
 * <ol>
 *   <li><strong>Tier-locked blocks</strong> — delegates to
 *       {@link dev.r3faced.minecurse.wasteland.managers.TierLockManager}
 *       which handles the bedrock + regen + drops + XP logic.</li>
 *   <li><strong>XP awarding</strong> — when the player holds the Mining Omni
 *       Tool and breaks a non-locked block that yields XP, XP is awarded.</li>
 * </ol>
 */
public class MiningListener implements Listener {

    private final WastelandPlugin plugin;

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Tier-lock check — runs at HIGHEST priority so it can cancel the break
     * before the MONITOR handler awards XP.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        // Delegate to the shared TierLockManager.
        plugin.getTierLockManager().handleBlockBreak(event, SkillType.MINING);
    }

    /**
     * XP awarding — runs at MONITOR priority, only if the break was not
     * cancelled (e.g. by the tier-lock check above).
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

        plugin.getSkillManager().awardXp(player, SkillType.MINING, xp, WastelandXpCause.BLOCK_BREAK, blockType);
    }
}
