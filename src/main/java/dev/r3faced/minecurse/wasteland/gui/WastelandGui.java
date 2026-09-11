package dev.r3faced.minecurse.wasteland.gui;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class WastelandGui implements InventoryHolder {

    protected final WastelandPlugin plugin;
    protected final Player player;
    protected Inventory inventory;

    protected WastelandGui(WastelandPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public abstract void build();

    public abstract void handleClick(InventoryClickEvent event);

    public void open() {
        build();
        player.openInventory(inventory);
    }

    protected Inventory createInventory(String title, int size) {
        inventory = Bukkit.createInventory(this, size, title);
        return inventory;
    }

    protected void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

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
