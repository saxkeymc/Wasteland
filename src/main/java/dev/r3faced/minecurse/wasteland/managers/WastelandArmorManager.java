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

public class WastelandArmorManager {

    private final WastelandPlugin plugin;

    public static final String NBT_KEY = "wasteland_armor";
    public static final String NBT_VALUE = "wasteland_set";

    private static final String SET_PREFIX = "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld ";

    public WastelandArmorManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    public void giveArmorSet(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (isWastelandArmor(item)) return;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWastelandArmor(item)) return;
        }

        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        String basePath = "armor-loadout." + player.getUniqueId();

        ItemStack helmet = loadPiece(cfg, basePath + ".helmet", Material.DIAMOND_HELMET, "Helmet");
        ItemStack chestplate = loadPiece(cfg, basePath + ".chestplate", Material.DIAMOND_CHESTPLATE, "Chestplate");
        ItemStack leggings = loadPiece(cfg, basePath + ".leggings", Material.DIAMOND_LEGGINGS, "Leggings");
        ItemStack boots = loadPiece(cfg, basePath + ".boots", Material.DIAMOND_BOOTS, "Boots");
        ItemStack sword = loadPiece(cfg, basePath + ".sword", Material.DIAMOND_SWORD, "Sword");

        if (helmet != null) player.getInventory().setHelmet(helmet);
        if (chestplate != null) player.getInventory().setChestplate(chestplate);
        if (leggings != null) player.getInventory().setLeggings(leggings);
        if (boots != null) player.getInventory().setBoots(boots);
        if (sword != null) player.getInventory().addItem(sword);

        if (cfg.contains(basePath + ".enchants")) {
            java.util.List<?> enchants = cfg.getList(basePath + ".enchants");
            if (enchants != null) {
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

    private ItemStack loadPiece(org.bukkit.configuration.file.FileConfiguration cfg, String path,
                                 Material mat, String name) {
        if (cfg.isSet(path)) {
            Object val = cfg.get(path);
            if (val instanceof String && "NONE".equals(val)) {
                return null;
            }
            ItemStack item = cfg.getItemStack(path);
            if (item != null && item.getType() != Material.AIR) {
                return dev.r3faced.minecurse.wasteland.nbt.NbtUtil.setTag(item, NBT_KEY, NBT_VALUE);
            }
            return null;
        }
        return buildPiece(mat, name,
                "&7&o\"Quote\"", "&7&o\"  — Author\"");
    }

    private void applyEnchantBook(Player player, ItemStack book) {
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) return;
        if (book.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            org.bukkit.inventory.meta.EnchantmentStorageMeta storage =
                    (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry :
                    storage.getStoredEnchants().entrySet()) {
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

    public void removeArmorSet(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isWastelandArmor(armor[i])) {
                armor[i] = null;
            }
        }
        player.getInventory().setArmorContents(armor);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isWastelandArmor(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }

    private ItemStack buildPiece(Material material, String pieceName, String... quotes) {
        String displayName = MessageUtil.colorize(SET_PREFIX + "&a&l" + pieceName);

        List<String> lore = new ArrayList<>();
        for (String q : quotes) {
            lore.add(MessageUtil.colorize(q));
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(displayName)
                .lore(lore);

        if (material.name().contains("SWORD")) {
            builder.enchant(Enchantment.DAMAGE_ALL, 5);
            builder.enchant(Enchantment.DURABILITY, 3);
            builder.enchant(Enchantment.FIRE_ASPECT, 2);
        } else {
            builder.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            builder.enchant(Enchantment.DURABILITY, 3);
        }

        builder.hideFlags();

        ItemStack item = builder.build();

        item = dev.r3faced.minecurse.wasteland.nbt.NbtUtil.setTag(item, NBT_KEY, NBT_VALUE);

        return item;
    }

    public boolean isWastelandArmor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String tag = dev.r3faced.minecurse.wasteland.nbt.NbtUtil.getTag(item, NBT_KEY);
        return NBT_VALUE.equals(tag);
    }
}
