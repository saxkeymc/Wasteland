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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interactive Armor Sets GUI — opened via /wasteland armorsets.
 * <p>
 * ONLY opens OUTSIDE Wasteland worlds.
 * <p>
 * Slots 10-14: Armor pieces (Helmet, Chestplate, Leggings, Boots, Sword).
 * Slots 19-25, 28-34: Enchant book slots.
 * Slot 40: Info. Slot 44: Close.
 * <p>
 * - Armor pieces can be taken out and replaced.
 * - Empty armor slots stay empty (no glass filling in).
 * - Only whitelisted enchants can be placed as books.
 * - The saved loadout is what WastelandArmorManager gives on world entry.
 */
public class ArmorSetsMenuGui extends WastelandGui {

    private static final int HELMET_SLOT = 10;
    private static final int CHESTPLATE_SLOT = 11;
    private static final int LEGGINGS_SLOT = 12;
    private static final int BOOTS_SLOT = 13;
    private static final int SWORD_SLOT = 14;
    private static final int INFO_SLOT = 40;
    private static final int CLOSE_SLOT = 44;

    private static final int[] ARMOR_SLOTS = {HELMET_SLOT, CHESTPLATE_SLOT, LEGGINGS_SLOT, BOOTS_SLOT, SWORD_SLOT};

    private static final int[] ENCHANT_SLOTS = {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    /** Slots that are editable (armor + enchant). */
    private static final int[] ALL_EDITABLE;
    static {
        ALL_EDITABLE = new int[ARMOR_SLOTS.length + ENCHANT_SLOTS.length];
        System.arraycopy(ARMOR_SLOTS, 0, ALL_EDITABLE, 0, ARMOR_SLOTS.length);
        System.arraycopy(ENCHANT_SLOTS, 0, ALL_EDITABLE, ARMOR_SLOTS.length, ENCHANT_SLOTS.length);
    }

    private static final Set<String> WHITELISTED_ENCHANTS = new HashSet<>();
    static {
        WHITELISTED_ENCHANTS.add("PROTECTION_ENVIRONMENTAL");
        WHITELISTED_ENCHANTS.add("PROTECTION_FIRE");
        WHITELISTED_ENCHANTS.add("PROTECTION_FALL");
        WHITELISTED_ENCHANTS.add("PROTECTION_EXPLOSIONS");
        WHITELISTED_ENCHANTS.add("PROTECTION_PROJECTILE");
        WHITELISTED_ENCHANTS.add("DURABILITY");
        WHITELISTED_ENCHANTS.add("DAMAGE_ALL");
        WHITELISTED_ENCHANTS.add("DAMAGE_UNDEAD");
        WHITELISTED_ENCHANTS.add("DAMAGE_ARTHROPODS");
        WHITELISTED_ENCHANTS.add("FIRE_ASPECT");
        WHITELISTED_ENCHANTS.add("KNOCKBACK");
        WHITELISTED_ENCHANTS.add("LOOT_BONUS_MOBS");
        WHITELISTED_ENCHANTS.add("LOOT_BONUS_BLOCKS");
        WHITELISTED_ENCHANTS.add("THORNS");
        WHITELISTED_ENCHANTS.add("WATER_WORKER");
        WHITELISTED_ENCHANTS.add("OXYGEN");
        WHITELISTED_ENCHANTS.add("DEPTH_STRIDER");
        WHITELISTED_ENCHANTS.add("SWEEPING_EDGE");
        WHITELISTED_ENCHANTS.add("DIG_SPEED");
        WHITELISTED_ENCHANTS.add("SILK_TOUCH");
    }

    public ArmorSetsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        String title = MessageUtil.colorize("&8Wasteland Armor Sets");
        createInventory(title, 45);

        // Fill ALL slots with dark glass first.
        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, darkPane);
        }

        // Load saved loadout — this OVERWRITES the glass in editable slots.
        loadLoadout();

        // Info.
        setItem(INFO_SLOT, new ItemBuilder(Material.SIGN)
                .name(MessageUtil.colorize("&2&lArmor Sets"))
                .lore(
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7Take armor pieces out — they stay removed."),
                    MessageUtil.colorize("&7Place new pieces in to replace them."),
                    MessageUtil.colorize("&7Drag enchanted books to apply enchants."),
                    MessageUtil.colorize("&7Only whitelisted enchants can be applied."),
                    MessageUtil.colorize(""),
                    MessageUtil.colorize("&7These changes apply when you enter"),
                    MessageUtil.colorize("&7a Wasteland world.")
                )
                .build());

        // Close.
        setItem(CLOSE_SLOT, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    private void loadLoadout() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String basePath = "armor-loadout." + player.getUniqueId();

        boolean hasLoadout = cfg.contains(basePath + ".helmet") ||
                             cfg.contains(basePath + ".chestplate") ||
                             cfg.contains(basePath + ".leggings") ||
                             cfg.contains(basePath + ".boots") ||
                             cfg.contains(basePath + ".sword");

        if (!hasLoadout) {
            // First time — create defaults.
            setItem(HELMET_SLOT, buildDefaultPiece(Material.DIAMOND_HELMET, "Helmet"));
            setItem(CHESTPLATE_SLOT, buildDefaultPiece(Material.DIAMOND_CHESTPLATE, "Chestplate"));
            setItem(LEGGINGS_SLOT, buildDefaultPiece(Material.DIAMOND_LEGGINGS, "Leggings"));
            setItem(BOOTS_SLOT, buildDefaultPiece(Material.DIAMOND_BOOTS, "Boots"));
            setItem(SWORD_SLOT, buildDefaultSword());
            saveLoadout();
            return;
        }

        // Load saved pieces — if a slot was emptied (null), leave it as glass.
        loadSlot(HELMET_SLOT, basePath + ".helmet");
        loadSlot(CHESTPLATE_SLOT, basePath + ".chestplate");
        loadSlot(LEGGINGS_SLOT, basePath + ".leggings");
        loadSlot(BOOTS_SLOT, basePath + ".boots");
        loadSlot(SWORD_SLOT, basePath + ".sword");

        // Load enchant books.
        if (cfg.contains(basePath + ".enchants")) {
            List<?> enchants = cfg.getList(basePath + ".enchants");
            if (enchants != null) {
                int slotIdx = 0;
                for (Object obj : enchants) {
                    if (obj instanceof ItemStack && slotIdx < ENCHANT_SLOTS.length) {
                        // Overwrite glass with the enchant book.
                        inventory.setItem(ENCHANT_SLOTS[slotIdx], (ItemStack) obj);
                        slotIdx++;
                    }
                }
            }
        }
    }

    private void loadSlot(int slot, String path) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        if (cfg.contains(path)) {
            ItemStack item = cfg.getItemStack(path);
            if (item != null && item.getType() != Material.AIR) {
                // Overwrite the glass with the saved item.
                inventory.setItem(slot, item);
            }
            // If null/AIR, the slot stays as glass (empty).
        }
    }

    private void saveLoadout() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String basePath = "armor-loadout." + player.getUniqueId();

        saveSlot(HELMET_SLOT, basePath + ".helmet");
        saveSlot(CHESTPLATE_SLOT, basePath + ".chestplate");
        saveSlot(LEGGINGS_SLOT, basePath + ".leggings");
        saveSlot(BOOTS_SLOT, basePath + ".boots");
        saveSlot(SWORD_SLOT, basePath + ".sword");

        // Save enchant books.
        List<ItemStack> enchantBooks = new ArrayList<>();
        for (int slot : ENCHANT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() == Material.ENCHANTED_BOOK) {
                enchantBooks.add(item);
            }
        }
        cfg.set(basePath + ".enchants", enchantBooks.isEmpty() ? null : enchantBooks);

        plugin.saveConfig();
    }

    private void saveSlot(int slot, String path) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        ItemStack item = inventory.getItem(slot);
        if (item != null && item.getType() != Material.STAINED_GLASS_PANE &&
            item.getType() != Material.AIR) {
            cfg.set(path, item);
        } else {
            cfg.set(path, null);
        }
    }

    private ItemStack buildDefaultPiece(Material mat, String name) {
        String displayName = MessageUtil.colorize(
                "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld &a&l" + name);
        return new ItemBuilder(mat)
                .name(displayName)
                .enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4)
                .enchant(Enchantment.DURABILITY, 3)
                .hideFlags()
                .build();
    }

    private ItemStack buildDefaultSword() {
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
            ItemStack clicked = inventory.getItem(slot);

            // If the clicked item is glass, info sign, or close barrier — cancel.
            if (clicked != null && (clicked.getType() == Material.STAINED_GLASS_PANE ||
                clicked.getType() == Material.BARRIER ||
                clicked.getType() == Material.SIGN)) {
                event.setCancelled(true);
                if (slot == CLOSE_SLOT) {
                    saveLoadout();
                    player.closeInventory();
                }
                return;
            }

            // If it's not an editable slot — cancel.
            if (!isEditableSlot(slot)) {
                event.setCancelled(true);
                return;
            }

            // Editable slot — check what's being placed.
            ItemStack cursor = event.getCursor();

            if (cursor != null && cursor.getType() != Material.AIR) {
                // Player is placing an item.
                if (isEnchantSlot(slot)) {
                    if (cursor.getType() != Material.ENCHANTED_BOOK) {
                        event.setCancelled(true);
                        player.sendMessage(MessageUtil.colorize("&cYou can only place enchanted books here."));
                        return;
                    }
                    if (!isEnchantWhitelisted(cursor)) {
                        event.setCancelled(true);
                        player.sendMessage(MessageUtil.colorize("&cYou cannot put this enchant on this item."));
                        return;
                    }
                }
                // Armor slots: allow any item to be placed (swap).
            }
            // Taking items from editable slots is allowed.
        }
        // Shift-click from player inventory — cancel to prevent shifting
        // items into our GUI randomly.
        if (slot >= 45 && event.getClick().isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // Save on any change (delayed).
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && event.getView().getTopInventory().getHolder() == this) {
                saveLoadout();
            }
        }, 1L);
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

    private boolean isEnchantWhitelisted(ItemStack book) {
        if (book == null) return false;
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return false;

        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta =
                    (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
            for (java.util.Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                String enchName = entry.getKey().getName().toUpperCase();
                if (!WHITELISTED_ENCHANTS.contains(enchName)) {
                    return false;
                }
            }
        }
        return true;
    }
}
