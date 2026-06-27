package dev.r3faced.minecurse.wasteland.gui;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all Wasteland GUI menus.
 * Subclasses implement {@link #build()} and {@link #handleClick(InventoryClickEvent)}.
 * The plugin's inventory listener delegates clicks to this class automatically.
 */
public abstract class WastelandGui implements InventoryHolder {

    protected final WastelandPlugin plugin;
    protected final Player player;
    protected Inventory inventory;

    protected WastelandGui(WastelandPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Build and populate the inventory. Called before opening.
     */
    public abstract void build();

    /**
     * Handle a click event in this inventory. Cancel to prevent item movement.
     */
    public abstract void handleClick(InventoryClickEvent event);

    /**
     * Open this GUI for the attached player.
     */
    public void open() {
        build();
        player.openInventory(inventory);
    }

    /**
     * Create the backing inventory with the given title and size.
     */
    protected Inventory createInventory(String title, int size) {
        inventory = Bukkit.createInventory(this, size, title);
        return inventory;
    }

    /**
     * Fill a slot with an item only if it's currently null.
     */
    protected void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    /**
     * Fill the entire inventory with a filler item, leaving named slots empty.
     */
    protected void fill(ItemStack filler) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
