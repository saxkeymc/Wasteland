package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.menus.DustUpgradeGui;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for right-clicks on Omni Tools and opens the Dust Upgrade GUI.
 */
public class ToolRightClickListener implements Listener {

    private final WastelandPlugin plugin;

    public ToolRightClickListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == org.bukkit.Material.AIR) return;

        // Check if the held item is an omni tool for any skill.
        for (SkillType skill : SkillType.values()) {
            if (plugin.getToolManager().isOmniTool(hand, skill)) {
                event.setCancelled(true);
                new DustUpgradeGui(plugin, player, skill).open();
                return;
            }
        }
    }
}
