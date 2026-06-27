package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Help GUI — a 3-row (27-slot) inventory opened via /wasteland help.
 * <p>
 * Shows a single configurable book item in the middle slot (default 13)
 * with all help text drawn from help.yml. A close button sits at the
 * bottom-middle (default 22).
 * <p>
 * Every property of every item (material, data, name, lore, slot,
 * enchants, flags, custom-model-data, skull-owner) is read directly
 * from the config section — nothing is hardcoded. Lore is read with
 * {@link ConfigurationSection#getStringList(String)}, which returns
 * every line regardless of length.
 */
public class HelpMenuGui extends WastelandGui {

    public HelpMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getHelp();
        String title = MessageUtil.colorize(cfg.getString("help-menu.title", "&7Wasteland &8\u2022 &aHelp"));
        int size = cfg.getInt("help-menu.size", 27);
        createInventory(title, size);

        // Border + filler — fully config-driven.
        ItemStack border = buildFiller(cfg, "help-menu.border-item", Material.STAINED_GLASS_PANE, (short) 15);
        ItemStack filler = buildFiller(cfg, "help-menu.fill-item",   Material.STAINED_GLASS_PANE, (short) 7);
        fill(filler);
        drawBorder(border);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        String playtime = PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());

        // ── Book item (middle slot) ───────────────────────────────────────────
        ConfigurationSection bookSec = cfg.getConfigurationSection("help-menu.book");
        if (bookSec != null) {
            int bookSlot = bookSec.getInt("slot", 13);
            ItemStack bookItem = buildConfigItem(bookSec, data, playtime);
            setItem(bookSlot, bookItem);
        }

        // ── Close button ──────────────────────────────────────────────────────
        ConfigurationSection closeSec = cfg.getConfigurationSection("help-menu.close");
        if (closeSec != null) {
            int closeSlot = closeSec.getInt("slot", 22);
            ItemStack closeItem = buildConfigItem(closeSec, data, playtime);
            setItem(closeSlot, closeItem);
        }
    }

    /**
     * Build an ItemStack from a config section, applying placeholders to
     * BOTH the name AND every lore line. Placeholders are substituted
     * BEFORE colorization so any {@code &} codes inside placeholder
     * values are correctly translated.
     */
    private ItemStack buildConfigItem(ConfigurationSection section, PlayerData data, String playtime) {
        // Read raw values from config.
        String matName = section.getString("material", "STONE");
        Material mat;
        try { mat = Material.valueOf(matName.toUpperCase()); }
        catch (Exception e) { mat = Material.STONE; }

        int dataVal = section.getInt("data", 0);
        int amount  = section.getInt("amount", 1);
        short dataShort = (short) Math.max(0, Math.min(dataVal, Short.MAX_VALUE));

        // Read name + lore as raw strings (placeholders not yet substituted).
        String rawName = section.getString("name");
        List<String> rawLore = section.isList("lore") ? section.getStringList("lore") : new ArrayList<String>();

        // Apply placeholders FIRST, then colorize.
        String name = rawName == null ? null : MessageUtil.colorize(applyPlaceholders(rawName, data, playtime));
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            // Substitute placeholders, THEN colorize — so &codes inside
            // placeholder values are also translated.
            lore.add(MessageUtil.colorize(applyPlaceholders(line, data, playtime)));
        }

        ItemBuilder b = new ItemBuilder(mat, amount, dataShort);
        if (name != null) b.name(name);
        if (!lore.isEmpty()) b.lore(lore);

        // Enchants
        if (section.isConfigurationSection("enchants")) {
            ConfigurationSection enchSec = section.getConfigurationSection("enchants");
            for (String enchName : enchSec.getKeys(false)) {
                int level = enchSec.getInt(enchName, 1);
                try {
                    org.bukkit.enchantments.Enchantment ench =
                            org.bukkit.enchantments.Enchantment.getByName(enchName.toUpperCase());
                    if (ench != null) b.enchant(ench, level);
                } catch (Exception ignored) {}
            }
        }

        // Item flags
        if (section.isList("item-flags")) {
            for (String flagName : section.getStringList("item-flags")) {
                try {
                    org.bukkit.inventory.ItemFlag flag =
                            org.bukkit.inventory.ItemFlag.valueOf(flagName.toUpperCase());
                    // Use reflection-safe path via ItemBuilder
                    b.hideFlags(); // simplified — apply all flags if any are listed
                    break;
                } catch (Exception ignored) {}
            }
        }

        // Glowing
        if (section.getBoolean("glowing", false)) b.glowing();

        // Hide flags
        if (section.getBoolean("hide-flags", false)) b.hideFlags();

        // Custom Model Data (1.14+; silently no-op on 1.8)
        int cmd = section.getInt("custom-model-data", -1);
        if (cmd >= 0) b.customModelData(cmd);

        // Skull owner
        if (mat == Material.SKULL_ITEM && dataShort == 3) {
            String owner = section.getString("skull-owner");
            if (owner != null) b.skullOwner(applyPlaceholders(owner, data, playtime));
        }

        return b.build();
    }

    /**
     * Substitute placeholders in a single string. Does NOT colorize —
     * the caller is responsible for calling {@link MessageUtil#colorize}
     * after substitution so any {@code &} codes inside placeholder
     * values are correctly translated.
     */
    private String applyPlaceholders(String input, PlayerData data, String playtime) {
        if (input == null) return null;
        return input
                .replace("{player}",      player.getName())
                .replace("{tier}",        String.valueOf(data.getTier()))
                .replace("{total_level}", String.valueOf(data.getTotalLevel()))
                .replace("{total_xp}",    String.valueOf(data.getTotalXp()))
                .replace("{playtime}",    playtime);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getHelp();
        int slot = event.getSlot();

        if (slot == cfg.getInt("help-menu.close.slot", 22)) {
            player.closeInventory();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    private ItemStack buildFiller(FileConfiguration cfg, String section, Material fallbackMat, short fallbackData) {
        String matName = cfg.getString(section + ".material", fallbackMat.name());
        int data = cfg.getInt(section + ".data", fallbackData);
        String name = cfg.getString(section + ".name", " ");
        return new ItemBuilder(parseMaterial(matName, fallbackMat), 1, (short) data).name(name).build();
    }

    private void drawBorder(ItemStack border) {
        int size = inventory.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int col = 0; col < cols; col++) {
            int top = col;
            int bottom = (rows - 1) * cols + col;
            if (inventory.getItem(top) == null)    inventory.setItem(top, border);
            if (inventory.getItem(bottom) == null) inventory.setItem(bottom, border);
        }
        for (int row = 0; row < rows; row++) {
            int left  = row * cols;
            int right = row * cols + (cols - 1);
            if (inventory.getItem(left) == null)  inventory.setItem(left, border);
            if (inventory.getItem(right) == null) inventory.setItem(right, border);
        }
    }
}
