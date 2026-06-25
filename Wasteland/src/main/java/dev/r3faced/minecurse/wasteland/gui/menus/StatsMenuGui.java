package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistics overview menu.
 */
public class StatsMenuGui extends WastelandGui {

    public StatsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        String title = MessageUtil.colorize(cfg.getString("stats-menu.title", "&2&lWasteland &8• &aStatistics"));
        createInventory(title, 54);

        // Filler
        String fillerMat = cfg.getString("stats-menu.fill-item.material", "STAINED_GLASS_PANE");
        int fillerData = cfg.getInt("stats-menu.fill-item.data", 7);
        ItemStack filler = new ItemBuilder(parseMat(fillerMat), 1, (short) fillerData).name(" ").build();
        fill(filler);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Per-skill items
        for (SkillType skill : SkillType.values()) {
            String cfgKey = skill.getKey();
            int slot = cfg.getInt("stats-menu.items." + cfgKey + ".slot", 20);
            Material mat = parseMat(cfg.getString("stats-menu.items." + cfgKey + ".material", "STONE"));
            String name = cfg.getString("stats-menu.items." + cfgKey + ".name", "&f" + cfgKey);

            List<String> lore = new ArrayList<>();
            for (String line : cfg.getStringList("stats-menu.items." + cfgKey + ".lore")) {
                line = line
                        .replace("{" + cfgKey + "_level}", String.valueOf(data.getLevel(skill)))
                        .replace("{" + cfgKey + "_xp}",    String.valueOf(data.getXp(skill)))
                        .replace("{tier}",                 String.valueOf(data.getTier()));
                lore.add(MessageUtil.colorize(line));
            }
            setItem(slot, new ItemBuilder(mat).name(name).lore(lore).build());
        }

        // Total
        int totalSlot = cfg.getInt("stats-menu.items.total.slot", 31);
        Material totalMat = parseMat(cfg.getString("stats-menu.items.total.material", "NETHER_STAR"));
        String totalName = MessageUtil.colorize(cfg.getString("stats-menu.items.total.name", "&e&lTotal Stats"));
        List<String> totalLore = new ArrayList<>();
        for (String line : cfg.getStringList("stats-menu.items.total.lore")) {
            line = line
                    .replace("{total_level}", String.valueOf(data.getTotalLevel()))
                    .replace("{total_xp}",    String.valueOf(data.getTotalXp()));
            totalLore.add(MessageUtil.colorize(line));
        }
        setItem(totalSlot, new ItemBuilder(totalMat).name(totalName).lore(totalLore).build());

        // Close
        int closeSlot = cfg.getInt("stats-menu.items.close.slot", 49);
        Material closeMat = parseMat(cfg.getString("stats-menu.items.close.material", "BARRIER"));
        String closeName = MessageUtil.colorize(cfg.getString("stats-menu.items.close.name", "&c&lClose"));
        setItem(closeSlot, new ItemBuilder(closeMat).name(closeName).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();
        if (slot == cfg.getInt("stats-menu.items.close.slot", 49)) {
            // Return to the Main Wasteland GUI instead of closing the inventory.
            new MainMenuGui(plugin, player).open();
        }
    }

    private Material parseMat(String name) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return Material.STONE; }
    }
}
