package dev.r3faced.minecurse.wasteland.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for constructing ItemStacks cleanly.
 * Designed to be 1.8.8 compatible.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount, short data) {
        this.item = new ItemStack(material, amount, data);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) meta.setDisplayName(MessageUtil.colorize(name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta != null) meta.setLore(MessageUtil.colorizeList(Arrays.asList(lines)));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null) meta.setLore(MessageUtil.colorizeList(lines));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null) meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder glowing() {
        if (meta != null) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            try {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Exception ignored) {}
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) {
            try {
                meta.addItemFlags(ItemFlag.values());
            } catch (Exception ignored) {}
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
