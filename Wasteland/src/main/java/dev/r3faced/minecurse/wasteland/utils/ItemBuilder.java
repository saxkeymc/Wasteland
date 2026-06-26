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

/**
 * Fluent builder for constructing ItemStacks cleanly.
 * <p>
 * Designed to be 1.8.8 compatible, with reflection-based fallbacks for
 * newer-version features (Custom Model Data on 1.14+, skull textures
 * on 1.8+ via offline-player owner).
 * <p>
 * Every setter is null-safe: passing null or a missing config value
 * leaves the corresponding item property unchanged rather than throwing.
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
        if (meta != null && name != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
        }
        return this;
    }

    /**
     * Set the lore from a varargs array of strings. Each line is colorized.
     * Replaces any existing lore.
     */
    public ItemBuilder lore(String... lines) {
        if (meta != null && lines != null) {
            meta.setLore(MessageUtil.colorizeList(Arrays.asList(lines)));
        }
        return this;
    }

    /**
     * Set the lore from a List of strings. Each line is colorized.
     * Replaces any existing lore. Null or empty lists clear the lore.
     * <p>
     * <strong>Important:</strong> if the lines have already been colorized
     * by an earlier step (e.g. placeholder substitution), this method is
     * still safe — ChatColor.translateAlternateColorCodes is idempotent.
     */
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

    /**
     * Append additional lore lines without replacing the existing lore.
     */
    public ItemBuilder addLore(List<String> lines) {
        if (meta != null && lines != null && !lines.isEmpty()) {
            List<String> current = meta.getLore();
            if (current == null) current = new ArrayList<>();
            else current = new ArrayList<>(current); // mutable copy
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

    /**
     * Apply a "glow" effect by adding a hidden Durability enchant.
     */
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

    /**
     * Set Custom Model Data (1.14+). Silently no-ops on 1.8.
     */
    public ItemBuilder customModelData(int data) {
        if (meta != null && data >= 0) {
            try {
                meta.getClass().getMethod("setCustomModelData", Integer.class)
                        .invoke(meta, data);
            } catch (Exception ignored) {}
        }
        return this;
    }

    /**
     * Set this item as a player-head with the given owner name.
     * Only effective if the material is SKULL_ITEM with data 3.
     */
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

    // ── Static config-driven factory ──────────────────────────────────────────

    /**
     * Build an ItemStack entirely from a configuration section.
     * <p>
     * Recognized keys (all optional):
     * <ul>
     *   <li>{@code material} (default STONE)</li>
     *   <li>{@code data} / {@code durability} (default 0)</li>
     *   <li>{@code amount} (default 1)</li>
     *   <li>{@code name} (default none)</li>
     *   <li>{@code lore} (default none — list of strings)</li>
     *   <li>{@code glowing} (default false)</li>
     *   <li>{@code enchants} (map: enchantment-name -> level)</li>
     *   <li>{@code item-flags} (list of ItemFlag names)</li>
     *   <li>{@code custom-model-data} (1.14+ only, default -1 = skip)</li>
     *   <li>{@code skull-owner} (string player name, only if material is SKULL_ITEM:3)</li>
     * </ul>
     * <p>
     * The {@code lore} list is read with {@link ConfigurationSection#getStringList(String)},
     * which returns ALL lines regardless of length — no truncation.
     *
     * @param section the config section (may be null — returns AIR)
     * @return the built ItemStack, or null if section is null
     */
    public static ItemStack fromConfig(ConfigurationSection section) {
        if (section == null) return null;

        // Material
        String matName = section.getString("material", "STONE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.STONE;
        }

        // Data + amount
        int data = section.getInt("data", section.getInt("durability", 0));
        int amount = section.getInt("amount", 1);
        short dataShort = (short) Math.max(0, Math.min(data, Short.MAX_VALUE));

        ItemBuilder b = new ItemBuilder(mat, amount, dataShort);

        // Name
        if (section.isString("name")) {
            b.name(section.getString("name"));
        }

        // Lore — read every line, no truncation. getStringList returns
        // the full list, even for very long lore sections.
        if (section.isList("lore")) {
            b.lore(section.getStringList("lore"));
        }

        // Enchants
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

        // Item flags
        if (section.isList("item-flags")) {
            for (String flagName : section.getStringList("item-flags")) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    if (b.meta != null) b.meta.addItemFlags(flag);
                } catch (Exception ignored) {}
            }
        }

        // Glowing
        if (section.getBoolean("glowing", false)) {
            b.glowing();
        }

        // Hide all flags
        if (section.getBoolean("hide-flags", false)) {
            b.hideFlags();
        }

        // Custom Model Data (1.14+)
        int cmd = section.getInt("custom-model-data", -1);
        if (cmd >= 0) {
            b.customModelData(cmd);
        }

        // Skull owner (only if material is SKULL_ITEM:3)
        if (mat == Material.SKULL_ITEM && dataShort == 3) {
            String owner = section.getString("skull-owner");
            if (owner != null) {
                b.skullOwner(owner);
            }
        }

        return b.build();
    }
}
