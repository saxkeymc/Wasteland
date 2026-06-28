package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Armor Sets GUI — shows the Wasteland armor set pieces (preview only).
 * Opened via /wasteland armorsets.
 * <p>
 * Layout: 3x9 (27 slots)
 * Slot 11: Helmet
 * Slot 12: Chestplate
 * Slot 13: Leggings
 * Slot 14: Boots
 * Slot 15: Sword
 * Slot 22: Close
 */
public class ArmorSetsMenuGui extends WastelandGui {

    public ArmorSetsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        String title = MessageUtil.colorize("&7Wasteland Armor Sets");
        createInventory(title, 27);

        // Border + filler
        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(border);

        // Armor pieces — preview what you get in a wasteland world.
        setArmorPiece(11, Material.DIAMOND_HELMET, "Helmet",
                "&7&o\"The mind is the helmet of the soul,\"",
                "&7&o\"  — Unknown\"");
        setArmorPiece(12, Material.DIAMOND_CHESTPLATE, "Chestplate",
                "&7&o\"Courage is the armor of the mind,\"",
                "&7&o\"  — Unknown\"");
        setArmorPiece(13, Material.DIAMOND_LEGGINGS, "Leggings",
                "&7&o\"Stand firm in what you believe,\"",
                "&7&o\"  — Unknown\"");
        setArmorPiece(14, Material.DIAMOND_BOOTS, "Boots",
                "&7&o\"Every journey begins with a single step,\"",
                "&7&o\"  — Lao Tzu\"");
        setArmorPiece(15, Material.DIAMOND_SWORD, "Sword",
                "&7&o\"The pen is mightier than the sword,\"",
                "&7&o\"  — Edward Bulwer-Lytton\"");

        // Close button
        setItem(22, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    private void setArmorPiece(int slot, Material mat, String name, String... quotes) {
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String q : quotes) lore.add(MessageUtil.colorize(q));
        lore.add("");
        lore.add(MessageUtil.colorize("&7This armor is given automatically"));
        lore.add(MessageUtil.colorize("&7when entering a Wasteland world."));
        lore.add(MessageUtil.colorize("&7Enchants: &aProt IV, Unbreaking III"));

        String displayName = MessageUtil.colorize(
                "&2&lW&a&la&2&ls&a&lt&2&le&a&lL&2&la&a&ln&2&ld &a&l" + name);

        ItemBuilder builder = new ItemBuilder(mat)
                .name(displayName)
                .lore(lore);

        // Add enchants to show in preview.
        if (mat.name().contains("SWORD")) {
            builder.enchant(org.bukkit.enchantments.Enchantment.DAMAGE_ALL, 5);
            builder.enchant(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
            builder.enchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
        } else {
            builder.enchant(org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            builder.enchant(org.bukkit.enchantments.Enchantment.DURABILITY, 3);
        }
        builder.hideFlags();

        setItem(slot, builder.build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();
        if (slot == 22) {
            player.closeInventory();
        }
    }

    private void drawBorder(ItemStack border) {
        int size = inventory.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int col = 0; col < cols; col++) {
            int top = col;
            int bottom = (rows - 1) * cols + col;
            if (inventory.getItem(top) == null) inventory.setItem(top, border);
            if (inventory.getItem(bottom) == null) inventory.setItem(bottom, border);
        }
        for (int row = 0; row < rows; row++) {
            int left = row * cols;
            int right = row * cols + (cols - 1);
            if (inventory.getItem(left) == null) inventory.setItem(left, border);
            if (inventory.getItem(right) == null) inventory.setItem(right, border);
        }
    }
}
