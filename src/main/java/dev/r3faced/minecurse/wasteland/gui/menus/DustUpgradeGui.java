package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Dust Upgrade GUI — opened when a player right-clicks their Omni Tool.
 * <p>
 * Shows the player's current dust balance and allows them to upgrade
 * the tool for the skill they're holding.
 * <p>
 * Layout: 3x9 (27 slots)
 * Slot 13: Tool info + current upgrade level
 * Slot 15: Upgrade button (shows cost + click to upgrade)
 * Slot 22: Close
 */
public class DustUpgradeGui extends WastelandGui {

    private final SkillType skill;

    public DustUpgradeGui(WastelandPlugin plugin, Player player, SkillType skill) {
        super(plugin, player);
        this.skill = skill;
    }

    @Override
    public void build() {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentUpgrade = data.getToolUpgradeLevel(skill);
        int nextUpgrade = currentUpgrade + 1;
        int cost = plugin.getDustManager().getUpgradeCost(nextUpgrade);
        int dust = data.getDust();
        boolean canUpgrade = nextUpgrade <= 4 && dust >= cost;
        boolean maxed = currentUpgrade >= 4;

        String skillName = skill.getKey().substring(0, 1).toUpperCase() + skill.getKey().substring(1);
        String title = MessageUtil.colorize("&7" + skillName + " Tool Upgrade");
        createInventory(title, 27);

        // Filler
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build());

        // Tool info (slot 13)
        ItemStack toolInfo = new ItemBuilder(Material.PAPER)
                .name(MessageUtil.colorize("&e&l" + skillName + " Tool"))
                .lore(
                        MessageUtil.colorize(""),
                        MessageUtil.colorize("&7Current Upgrade: &aLevel " + currentUpgrade),
                        MessageUtil.colorize("&7Dust Balance: &e" + dust),
                        MessageUtil.colorize("")
                )
                .build();
        setItem(13, toolInfo);

        // Upgrade button (slot 15)
        if (maxed) {
            setItem(15, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&c&lMaxed Out"))
                    .lore(MessageUtil.colorize("&7This tool is fully upgraded!"))
                    .build());
        } else {
            Material buttonMat = canUpgrade ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            String status = canUpgrade ? "&aClick to upgrade!" : "&cNot enough dust!";
            setItem(15, new ItemBuilder(buttonMat)
                    .name(MessageUtil.colorize("&e&lUpgrade to Level " + nextUpgrade))
                    .lore(
                            MessageUtil.colorize(""),
                            MessageUtil.colorize("&7Cost: &e" + cost + " dust"),
                            MessageUtil.colorize("&7You have: &e" + dust + " dust"),
                            MessageUtil.colorize(""),
                            MessageUtil.colorize(status)
                    )
                    .build());
        }

        // Close (slot 22)
        setItem(22, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();

        if (slot == 15) {
            // Try to upgrade.
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (plugin.getDustManager().upgradeTool(data, skill)) {
                plugin.getDataManager().savePlayer(player.getUniqueId());
                player.sendMessage(MessageUtil.colorize("&a&lUpgrade successful! &7Your tool is now Level " +
                        data.getToolUpgradeLevel(skill) + "."));
                // Refresh the GUI.
                new DustUpgradeGui(plugin, player, skill).open();
            } else {
                int current = data.getToolUpgradeLevel(skill);
                int next = current + 1;
                int cost = plugin.getDustManager().getUpgradeCost(next);
                if (current >= 4) {
                    player.sendMessage(MessageUtil.colorize("&cThis tool is already maxed out!"));
                } else {
                    player.sendMessage(MessageUtil.colorize("&cYou need &4" + cost + " dust &cto upgrade! " +
                            "You only have &4" + data.getDust() + "&c."));
                }
            }
        } else if (slot == 22) {
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
