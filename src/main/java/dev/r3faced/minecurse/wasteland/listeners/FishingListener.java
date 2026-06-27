package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Awards Fishing XP when the player catches something with the Fishing Omni Tool.
 * XP is determined by the type of item caught; falls back to DEFAULT.
 */
public class FishingListener implements Listener {

    private final WastelandPlugin plugin;

    public FishingListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();

        // Verify world
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FISHING) return;

        // Verify the player holds the Fishing Omni Tool
        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) return;

        // Determine caught item type
        String caughtType = "DEFAULT";
        if (event.getCaught() instanceof Item) {
            ItemStack caught = ((Item) event.getCaught()).getItemStack();
            caughtType = caught.getType().name();
        }

        int xp = plugin.getSkillManager().getXpForBlock(SkillType.FISHING, caughtType);
        if (xp <= 0) return;

        plugin.getSkillManager().awardXp(player, SkillType.FISHING, xp, WastelandXpCause.FISHING, caughtType);
    }
}
