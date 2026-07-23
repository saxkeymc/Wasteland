package dev.r3faced.minecurse.wasteland.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
        if (meta != null && name != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta != null && lines != null) {
            meta.setLore(MessageUtil.colorizeList(Arrays.asList(lines)));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null) {
            if (lines == null || lines.isEmpty()) {
                meta.setLore(null);
            } else {
                meta.setLore(MessageUtil.colorizeList(lines));
            }
        }
        return this;
    }

    public ItemBuilder addLore(List<String> lines) {
        if (meta != null && lines != null && !lines.isEmpty()) {
            List<String> current = meta.getLore();
            if (current == null) current = new ArrayList<>();
            else current = new ArrayList<>(current);
            current.addAll(MessageUtil.colorizeList(lines));
            meta.setLore(current);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null && enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
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

    public ItemBuilder customModelData(int data) {
        if (meta != null && data >= 0) {
            try {
                meta.getClass().getMethod("setCustomModelData", Integer.class)
                        .invoke(meta, data);
            } catch (Exception ignored) {}
        }
        return this;
    }

    public ItemBuilder skullOwner(String ownerName) {
        if (meta != null && meta instanceof SkullMeta && ownerName != null) {
            try {
                ((SkullMeta) meta).setOwner(ownerName);
            } catch (Exception ignored) {}
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }

    public static ItemStack fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        String matName = section.getString("material", "STONE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        int data = section.getInt("data", section.getInt("durability", 0));
        int amount = section.getInt("amount", 1);
        short dataShort = (short) Math.max(0, Math.min(data, Short.MAX_VALUE));

        ItemBuilder b = new ItemBuilder(mat, amount, dataShort);

        if (section.isString("name")) {
            b.name(section.getString("name"));
        }

        if (section.isList("lore")) {
            b.lore(section.getStringList("lore"));
        }

        if (section.isConfigurationSection("enchants")) {
            ConfigurationSection enchSec = section.getConfigurationSection("enchants");
            for (String enchName : enchSec.getKeys(false)) {
                int level = enchSec.getInt(enchName, 1);
                try {
                    Enchantment ench = Enchantment.getByName(enchName.toUpperCase());
                    if (ench != null) b.enchant(ench, level);
                } catch (Exception ignored) {}
            }
        }

        if (section.isList("item-flags")) {
            for (String flagName : section.getStringList("item-flags")) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    if (b.meta != null) b.meta.addItemFlags(flag);
                } catch (Exception ignored) {}
            }
        }

        if (section.getBoolean("glowing", false)) {
            b.glowing();
        }

        if (section.getBoolean("hide-flags", false)) {
            b.hideFlags();
        }

        int cmd = section.getInt("custom-model-data", -1);
        if (cmd >= 0) {
            b.customModelData(cmd);
        }

        if (mat == Material.SKULL_ITEM && dataShort == 3) {
            String owner = section.getString("skull-owner");
            if (owner != null) {
                b.skullOwner(owner);
            }
        }

        return b.build();
    }
}
