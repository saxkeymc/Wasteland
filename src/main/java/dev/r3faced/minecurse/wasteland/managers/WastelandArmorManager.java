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

        // Create and equip the armor set.
        ItemStack helmet = buildPiece(Material.DIAMOND_HELMET, "Helmet",
                "&7&o\"The mind is the helmet of the soul,\"",
                "&7&o\"  — Unknown\"");
        ItemStack chestplate = buildPiece(Material.DIAMOND_CHESTPLATE, "Chestplate",
                "&7&o\"Courage is the armor of the mind,\"",
                "&7&o\"  — Unknown\"");
        ItemStack leggings = buildPiece(Material.DIAMOND_LEGGINGS, "Leggings",
                "&7&o\"Stand firm in what you believe,\"",
                "&7&o\"  — Unknown\"");
        ItemStack boots = buildPiece(Material.DIAMOND_BOOTS, "Boots",
                "&7&o\"Every journey begins with a single step,\"",
                "&7&o\"  — Lao Tzu\"");
        ItemStack sword = buildPiece(Material.DIAMOND_SWORD, "Sword",
                "&7&o\"The pen is mightier than the sword,\"",
                "&7&o\"  — Edward Bulwer-Lytton\"");

        // Equip armor.
        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // Give sword in the first available hotbar slot.
        player.getInventory().addItem(sword);
        player.updateInventory();
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
