package dev.r3faced.minecurse.wasteland.gui;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listens for inventory click events and delegates them to the appropriate
 * WastelandGui subclass if the clicked inventory belongs to one.
 */
public class GuiListener implements Listener {

    private final WastelandPlugin plugin;

    public GuiListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof WastelandGui) {
            WastelandGui gui = (WastelandGui) holder;
            // ArmorSetsMenuGui and BackpackMenuGui are interactive — they handle
            // their own click cancellation internally.
            if (gui instanceof dev.r3faced.minecurse.wasteland.gui.menus.ArmorSetsMenuGui ||
                gui instanceof dev.r3faced.minecurse.wasteland.gui.menus.BackpackMenuGui) {
                gui.handleClick(event);
            } else {
                event.setCancelled(true);
                gui.handleClick(event);
            }
        }
    }
}
