package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Wasteland armor set given to players when they enter a
 * Wasteland world.
 * <p>
 * The set consists of:
 * <ul>
 *   <li>Helmet — Diamond Helmet</li>
 *   <li>Chestplate — Diamond Chestplate</li>
 *   <li>Leggings — Diamond Leggings</li>
 *   <li>Boots — Diamond Boots</li>
 *   <li>Sword — Diamond Sword</li>
 * </ul>
 * <p>
 * Each piece has a custom name with the alternating green/aqua color
 * scheme and themed quote lore. All enchants are hidden.
 * <p>
 * The set is removed when the player leaves a Wasteland world.
 */
public class WastelandArmorManager {

    private final WastelandPlugin plugin;

    /** NBT key used to identify Wasteland armor pieces. */
    public static final String NBT_KEY = "wasteland_armor";
    public static final String NBT_VALUE = "wasteland_set";

    /** The alternating-color prefix for all piece names. */
    private static final String SET_PREFIX = "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld ";

    public WastelandArmorManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Give the player the full Wasteland armor set + sword.
     * Called when entering a Wasteland world.
     */
    public void giveArmorSet(Player player) {
        // Check if the player already has any Wasteland armor piece.
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isWastelandArmor(item)) return; // Already has the set.
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWastelandArmor(item)) return;
        }

        // Load the player's saved loadout from config.
        // If no saved loadout, use defaults.
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String basePath = "armor-loadout." + player.getUniqueId();

        ItemStack helmet = loadPiece(cfg, basePath + ".helmet", Material.DIAMOND_HELMET, "Helmet");
        ItemStack chestplate = loadPiece(cfg, basePath + ".chestplate", Material.DIAMOND_CHESTPLATE, "Chestplate");
        ItemStack leggings = loadPiece(cfg, basePath + ".leggings", Material.DIAMOND_LEGGINGS, "Leggings");
        ItemStack boots = loadPiece(cfg, basePath + ".boots", Material.DIAMOND_BOOTS, "Boots");
        ItemStack sword = loadPiece(cfg, basePath + ".sword", Material.DIAMOND_SWORD, "Sword");

        // Equip armor (only if not null — player may have removed it).
        if (helmet != null) player.getInventory().setHelmet(helmet);
        if (chestplate != null) player.getInventory().setChestplate(chestplate);
        if (leggings != null) player.getInventory().setLeggings(leggings);
        if (boots != null) player.getInventory().setBoots(boots);
        if (sword != null) player.getInventory().addItem(sword);

        // Also apply enchant books from the loadout.
        if (cfg.contains(basePath + ".enchants")) {
            java.util.List<?> enchants = cfg.getList(basePath + ".enchants");
            if (enchants != null) {
                // Apply each enchant book to the appropriate armor piece.
                for (Object obj : enchants) {
                    if (obj instanceof ItemStack) {
                        ItemStack book = (ItemStack) obj;
                        applyEnchantBook(player, book);
                    }
                }
            }
        }

        player.updateInventory();
    }

    /** Load a piece from config. Returns null if the player removed it. */
    private ItemStack loadPiece(org.bukkit.configuration.file.FileConfiguration cfg, String path,
                                 Material mat, String name) {
        // Check if the player has a saved loadout entry for this slot.
        if (cfg.isSet(path)) {
            // The path exists — it could be a saved item or null (removed).
            ItemStack item = cfg.getItemStack(path);
            if (item != null && item.getType() != Material.AIR) {
                // Tag it as Wasteland armor so it can be removed on leave.
                return dev.r3faced.minecurse.wasteland.nbt.NbtUtil.setTag(item, NBT_KEY, NBT_VALUE);
            }
            // Item is null or AIR — player removed this piece. Return null.
            return null;
        }
        // Path doesn't exist at all — first time, give default.
        return buildPiece(mat, name,
                "&7&o\"Quote\"", "&7&o\"  — Author\"");
    }

    /** Apply an enchant book's stored enchants to the player's armor. */
    private void applyEnchantBook(Player player, ItemStack book) {
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) return;
        if (book.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta storage =
                    (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry :
                    storage.getStoredEnchants().entrySet()) {
                // Apply to all armor pieces + sword.
                applyToAllArmor(player, entry.getKey(), entry.getValue());
            }
        }
    }

    private void applyToAllArmor(Player player, org.bukkit.enchantments.Enchantment ench, int level) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isWastelandArmor(armor[i])) {
                org.bukkit.inventory.meta.ItemMeta meta = armor[i].getItemMeta();
                if (meta != null) {
                    meta.addEnchant(ench, level, true);
                    armor[i].setItemMeta(meta);
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        // Also apply to the sword (first Wasteland sword in inventory).
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isWastelandArmor(item) && item.getType().name().contains("SWORD")) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(ench, level, true);
                    item.setItemMeta(meta);
                    player.getInventory().setItem(i, item);
                }
                break;
            }
        }
    }

    /**
     * Remove ALL Wasteland armor pieces from the player's inventory.
     * Called when leaving a Wasteland world.
     */
    public void removeArmorSet(Player player) {
        // Remove from armor slots.
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isWastelandArmor(armor[i])) {
                armor[i] = null;
            }
        }
        player.getInventory().setArmorContents(armor);

        // Remove from main inventory.
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isWastelandArmor(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }

    /**
     * Build a single Wasteland armor piece with the custom name, lore,
     * enchants, and hidden flags.
     */
    private ItemStack buildPiece(Material material, String pieceName, String... quotes) {
        String displayName = MessageUtil.colorize(SET_PREFIX + "&a&l" + pieceName);

        List<String> lore = new ArrayList<>();
        for (String q : quotes) {
            lore.add(MessageUtil.colorize(q));
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(displayName)
                .lore(lore);

        // Add enchants.
        if (material.name().contains("SWORD")) {
            builder.enchant(Enchantment.DAMAGE_ALL, 5);
            builder.enchant(Enchantment.DURABILITY, 3);
            builder.enchant(Enchantment.FIRE_ASPECT, 2);
        } else {
            builder.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            builder.enchant(Enchantment.DURABILITY, 3);
        }

        // Hide all flags.
        builder.hideFlags();

        ItemStack item = builder.build();

        // Add NBT tag for identification.
        item = dev.r3faced.minecurse.wasteland.nbt.NbtUtil.setTag(item, NBT_KEY, NBT_VALUE);

        return item;
    }

    /**
     * Check if an ItemStack is a Wasteland armor piece.
     */
    public boolean isWastelandArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String tag = dev.r3faced.minecurse.wasteland.nbt.NbtUtil.getTag(item, NBT_KEY);
        return NBT_VALUE.equals(tag);
    }
}
