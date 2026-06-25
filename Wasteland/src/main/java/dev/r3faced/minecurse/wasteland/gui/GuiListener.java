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
            event.setCancelled(true);
            ((WastelandGui) holder).handleClick(event);
        }
    }
}
