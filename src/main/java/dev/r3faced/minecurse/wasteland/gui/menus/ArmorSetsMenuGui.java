package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive Armor Sets GUI — opened via /wasteland armorsets.
 * <p>
 * Players can drag items from their inventory into this GUI to customize
 * their Wasteland armor set. When they close the GUI, the items are saved
 * and used as the armor set the player receives when entering a Wasteland world.
 * <p>
 * Layout: 5x9 (45 slots)
 * Slots 0-3: Armor slots (Helmet, Chestplate, Leggings, Boots)
 * Slot 4: Sword
 * Slots 9-35: Open for dragging enchant books / items
 * Slot 40: Info
 * Slot 44: Close
 */
public class ArmorSetsMenuGui extends WastelandGui {

    private static final int HELMET_SLOT = 0;
    private static final int CHESTPLATE_SLOT = 1;
    private static final int LEGGINGS_SLOT = 2;
    private static final int BOOTS_SLOT = 3;
    private static final int SWORD_SLOT = 4;
    private static final int CLOSE_SLOT = 44;
    private static final int INFO_SLOT = 40;

    /** Slots where players can place items (0-35, excluding fixed slots). */
    private static final int[] EDITABLE_SLOTS = {
        HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, SWORD_SLOT,
        9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29, 30, 31, 32, 33, 34, 35
    };

    public ArmorSetsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        String title = MessageUtil.colorize("&8Wasteland Armor Sets");
        createInventory(title, 45);

        // Fill non-editable slots with dark glass.
        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        for (int i = 0; i < 45; i++) {
            if (!isEditableSlot(i)) {
                inventory.setItem(i, darkPane);
            }
        }

        // Info item.
        setItem(INFO_SLOT, new ItemBuilder(Material.SIGN)
                .name(MessageUtil.colorize("&2&lInstructions"))
                .lore(
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7Place items in slots 0-4 for:"),
                    MessageUtil.colorize("&7Helmet, Chestplate, Leggings, Boots, Sword"),
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7Place enchanted books in other slots"),
                    MessageUtil.colorize("&7to apply enchants to your armor."),
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7Close this GUI to save your loadout.")
                )
                .build());

        // Close button.
        setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose & Save"))
                .lore(MessageUtil.colorize("&7Click to save and close."))
                .build());

        // Load saved loadout from config.
        loadSavedLoadout();
    }

    private boolean isEditableSlot(int slot) {
        for (int s : EDITABLE_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private void loadSavedLoadout() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        for (int i = 0; i < EDITABLE_SLOTS.length; i++) {
            int slot = EDITABLE_SLOTS[i];
            String path = "armor-loadout." + player.getUniqueId() + ".slot_" + i;
            if (cfg.contains(path)) {
                ItemStack item = cfg.getItemStack(path);
                if (item != null) {
                    inventory.setItem(slot, item);
                }
            }
        }
    }

    private void saveLoadout() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String basePath = "armor-loadout." + player.getUniqueId();
        // Clear old loadout.
        cfg.set(basePath, null);

        for (int i = 0; i < EDITABLE_SLOTS.length; i++) {
            int slot = EDITABLE_SLOTS[i];
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                cfg.set(basePath + ".slot_" + i, item);
            }
        }
        plugin.saveConfig();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // This GUI is interactive — players can move items in and out.
        // We only cancel clicks on non-editable slots.
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < 45) {
            // Clicked inside our inventory.
            if (!isEditableSlot(slot)) {
                // Non-editable slot — cancel.
                event.setCancelled(true);
                if (slot == CLOSE_SLOT) {
                    saveLoadout();
                    player.closeInventory();
                }
            }
            // Editable slots — allow the click (don't cancel).
        }
        // Clicks in the player's own inventory (slot >= 45) are allowed.
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (event.getInventory().getHolder() != this) return;

        // Save the loadout when the GUI is closed.
        saveLoadout();
        player.sendMessage(MessageUtil.colorize("&2&lSaved! &7Your armor loadout has been updated."));
    }
}
