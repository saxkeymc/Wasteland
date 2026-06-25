package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import org.bukkit.Material;
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
 * The rest of the inventory is filled with decorative glass panes;
 * the outer ring uses a darker pane for a clean border.
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

        // Border + filler
        ItemStack border = buildFiller(cfg, "help-menu.border-item", Material.STAINED_GLASS_PANE, (short) 15);
        ItemStack filler = buildFiller(cfg, "help-menu.fill-item",   Material.STAINED_GLASS_PANE, (short) 7);
        fill(filler);
        drawBorder(border);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        String playtime = PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());

        // ── Book item (middle slot) ───────────────────────────────────────────
        int bookSlot = cfg.getInt("help-menu.book.slot", 13);
        Material bookMat = parseMaterial(cfg.getString("help-menu.book.material", "BOOK"), Material.BOOK);
        int bookData = cfg.getInt("help-menu.book.data", 0);
        String bookName = cfg.getString("help-menu.book.name", "&a&lWasteland Help");
        List<String> bookLore = applyPlaceholders(cfg.getStringList("help-menu.book.lore"), data, playtime);
        setItem(bookSlot, new ItemBuilder(bookMat, 1, (short) bookData).name(bookName).lore(bookLore).build());

        // ── Close button ──────────────────────────────────────────────────────
        int closeSlot = cfg.getInt("help-menu.close.slot", 22);
        Material closeMat = parseMaterial(cfg.getString("help-menu.close.material", "BARRIER"), Material.BARRIER);
        int closeData = cfg.getInt("help-menu.close.data", 0);
        String closeName = cfg.getString("help-menu.close.name", "&c&lClose");
        List<String> closeLore = MessageUtil.colorizeList(cfg.getStringList("help-menu.close.lore"));
        setItem(closeSlot, new ItemBuilder(closeMat, 1, (short) closeData).name(closeName).lore(closeLore).build());
    }

    private List<String> applyPlaceholders(List<String> raw, PlayerData data, String playtime) {
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            String l = MessageUtil.colorize(line)
                    .replace("{player}",      player.getName())
                    .replace("{tier}",        String.valueOf(data.getTier()))
                    .replace("{total_level}", String.valueOf(data.getTotalLevel()))
                    .replace("{playtime}",    playtime);
            out.add(l);
        }
        return out;
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
