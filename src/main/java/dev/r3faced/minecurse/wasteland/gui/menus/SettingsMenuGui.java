package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SettingsMenuGui extends WastelandGui {

    public SettingsMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        String title = MessageUtil.colorize(cfg.getString("settings-menu.title", "&7Wasteland Settings"));
        int size = cfg.getInt("settings-menu.size", 27);
        createInventory(title, size);

        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(border);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        boolean seePlayers = data.isSettingSeePlayers();
        setToggleItem(11, "See Players in Wasteland", seePlayers,
                "&7Toggle whether you can see other",
                "&7players while in Wasteland worlds.");

        boolean xpNoises = data.isSettingXpNoises();
        setToggleItem(13, "XP Noises", xpNoises,
                "&7Toggle whether you hear an XP",
                "&7orb sound when using your tools.");

        boolean xpBar = data.isSettingXpBarDisplay();
        setToggleItem(15, "Level on XP Bar", xpBar,
                "&7Toggle whether your Wasteland level",
                "&7is shown on your XP bar.");

        setItem(22, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    private void setToggleItem(int slot, String name, boolean enabled, String... desc) {
        Material mat = enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String status = enabled ? "&a&lON" : "&c&lOFF";
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String d : desc) lore.add(d);
        lore.add("");
        lore.add(MessageUtil.colorize("&7Status: " + status));
        lore.add(MessageUtil.colorize("&eClick to toggle."));

        setItem(slot, new ItemBuilder(mat)
                .name(MessageUtil.colorize("&e&l" + name))
                .lore(lore)
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (slot == 11) {
            data.setSettingSeePlayers(!data.isSettingSeePlayers());
            plugin.getDataManager().savePlayer(player.getUniqueId());
            applyPlayerVisibility(player, data.isSettingSeePlayers());
            new SettingsMenuGui(plugin, player).open();
        } else if (slot == 13) {
            data.setSettingXpNoises(!data.isSettingXpNoises());
            plugin.getDataManager().savePlayer(player.getUniqueId());
            new SettingsMenuGui(plugin, player).open();
        } else if (slot == 15) {
            data.setSettingXpBarDisplay(!data.isSettingXpBarDisplay());
            plugin.getDataManager().savePlayer(player.getUniqueId());
            new SettingsMenuGui(plugin, player).open();
        } else if (slot == 22) {
            player.closeInventory();
        }
    }

    private void applyPlayerVisibility(Player player, boolean seePlayers) {
        for (Player other : player.getWorld().getPlayers()) {
            if (other == player) continue;
            if (seePlayers) {
                player.showPlayer(other);
            } else {
                player.hidePlayer(other);
            }
        }
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
