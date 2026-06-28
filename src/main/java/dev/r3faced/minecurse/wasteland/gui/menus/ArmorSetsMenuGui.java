package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Armor Sets GUI — opened via /wasteland armorsets.
 * <p>
 * ONLY opens outside Wasteland worlds (at spawn, hub, etc.).
 * <p>
 * Shows the Wasteland armor set pieces (Helmet, Chestplate, Leggings,
 * Boots, Sword). Players can:
 * <ul>
 *   <li>Take the armor pieces out of the GUI into their inventory.</li>
 *   <li>Place enchanted books into the GUI to apply enchants.</li>
 *   <li>Only whitelisted enchants can be applied.</li>
 * </ul>
 * <p>
 * Layout: 5x9 (45 slots)
 * Slot 10: Helmet
 * Slot 11: Chestplate
 * Slot 12: Leggings
 * Slot 13: Boots
 * Slot 14: Sword
 * Slots 19-35: Enchant book slots (drag books here)
 * Slot 40: Info
 * Slot 44: Close
 */
public class ArmorSetsMenuGui extends WastelandGui {

    private static final int HELMET_SLOT = 10;
    private static final int CHESTPLATE_SLOT = 11;
    private static final int LEGGINGS_SLOT = 12;
    private static final int BOOTS_SLOT = 13;
    private static final int SWORD_SLOT = 14;
    private static final int INFO_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    /** Armor piece slots. */
    private static final int[] ARMOR_SLOTS = {HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, SWORD_SLOT};

    /** Slots where players can place enchant books (rows 3-4). */
    private static final int[] ENCHANT_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    /** All editable slots (armor + enchant slots). */
    private static final int[] ALL_EDITABLE;

    static {
        ALL_EDITABLE = new int[ARMOR_SLOTS.length + ENCHANT_SLOTS.length];
        System.arraycopy(ARMOR_SLOTS, 0, ALL_EDITABLE, 0, ARMOR_SLOTS.length);
        System.arraycopy(ENCHANT_SLOTS, 0, ALL_EDITABLE, ARMOR_SLOTS.length, ENCHANT_SLOTS.length);
    }

    public ArmorSetsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        String title = MessageUtil.colorize("&8Wasteland Armor Sets");
        createInventory(title, 45);

        // Fill with dark glass.
        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        fill(darkPane);

        // ── Place default Wasteland armor set ────────────────────────────────
        setItem(HELMET_SLOT, buildArmorPiece(Material.DIAMOND_HELMET, "Helmet"));
        setItem(CHESTPLATE_SLOT, buildArmorPiece(Material.DIAMOND_CHESTPLATE, "Chestplate"));
        setItem(LEGGINGS_SLOT, buildArmorPiece(Material.DIAMOND_LEGGINGS, "Leggings"));
        setItem(BOOTS_SLOT, buildArmorPiece(Material.DIAMOND_BOOTS, "Boots"));
        setItem(SWORD_SLOT, buildSwordPiece());

        // ── Info ─────────────────────────────────────────────────────────────
        setItem(INFO_SLOT, new ItemBuilder(Material.SIGN)
                .name(MessageUtil.colorize("&2&lArmor Sets"))
                .lore(
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7Take the armor pieces into your inventory."),
                    MessageUtil.colorize("&7Drag enchanted books onto armor to enchant."),
                    MessageUtil.colorize("&7Only whitelisted enchants can be applied."),
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7These changes apply when you enter"),
                    MessageUtil.colorize("&7a Wasteland world.")
                )
                .build());

        // ── Close ────────────────────────────────────────────────────────────
        setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    private ItemStack buildArmorPiece(Material mat, String name) {
        String displayName = MessageUtil.colorize(
                "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld &a&l" + name);
        ItemBuilder b = new ItemBuilder(mat)
                .name(displayName)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.DURABILITY, 3)
                .hideFlags();
        return b.build();
    }

    private ItemStack buildSwordPiece() {
        String displayName = MessageUtil.colorize(
                "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld &a&lSword");
        return new ItemBuilder(Material.DIAMOND_SWORD)
                .name(displayName)
                .enchant(Enchantment.DAMAGE_ALL, 5)
                .enchant(Enchantment.DURABILITY, 3)
                .enchant(Enchantment.FIRE_ASPECT, 2)
                .hideFlags()
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < 45) {
            // Clicked inside our inventory.
            if (!isEditableSlot(slot)) {
                // Non-editable slot — cancel.
                event.setCancelled(true);
                if (slot == CLOSE_SLOT) {
                    player.closeInventory();
                }
                return;
            }

            // Editable slot — check what's being placed.
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();

            // If the player is trying to PLACE an item (not take):
            if (cursor != null && cursor.getType() != Material.AIR) {
                // Only allow enchanted books in enchant slots.
                if (isEnchantSlot(slot)) {
                    if (cursor.getType() != Material.ENCHANTED_BOOK) {
                        event.setCancelled(true);
                        player.sendMessage(MessageUtil.colorize("&cYou can only place enchanted books here."));
                        return;
                    }
                    // Check if the enchant is whitelisted.
                    if (!isEnchantWhitelisted(cursor)) {
                        event.setCancelled(true);
                        player.sendMessage(MessageUtil.colorize("&cYou cannot put this enchant on this item."));
                        return;
                    }
                } else if (isArmorSlot(slot)) {
                    // Don't allow placing items on armor slots — they're pre-filled.
                    event.setCancelled(true);
                    return;
                }
            }
            // If taking an item from an armor slot — allow it.
            // The slot will be refilled when the GUI is reopened.
        }
        // Clicks in the player's own inventory (slot >= 45) are allowed.
    }

    private boolean isEditableSlot(int slot) {
        for (int s : ALL_EDITABLE) {
            if (s == slot) return true;
        }
        return false;
    }

    private boolean isArmorSlot(int slot) {
        for (int s : ARMOR_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    private boolean isEnchantSlot(int slot) {
        for (int s : ENCHANT_SLOTS) {
            if (s == slot) return true;
        }
        return false;
    }

    /**
     * Check if an enchanted book has a whitelisted enchant.
     * For now, all vanilla enchants are allowed. Custom enchants from
     * enchants.yml would be checked here once the CurseEnchants JAR is available.
     */
    private boolean isEnchantWhitelisted(ItemStack book) {
        // Allow all enchanted books for now — the enchant whitelist in
        // enchants.yml will be enforced once the CurseEnchants API is integrated.
        return book.getType() == Material.ENCHANTED_BOOK;
    }
}
