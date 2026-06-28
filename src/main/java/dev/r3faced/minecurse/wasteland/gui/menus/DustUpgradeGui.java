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

import java.util.ArrayList;
import java.util.List;

/**
 * Dust Upgrade GUI — opened when a player right-clicks their Omni Tool.
 * <p>
 * Shows the player's current dust balance and allows them to upgrade
 * the tool for the skill they're holding.
 * <p>
 * Layout: 4x9 (36 slots)
 * Slot 4:  Tool info (current upgrade level)
 * Slot 13: Dust balance
 * Slot 22: Upgrade button (shows cost + click to upgrade)
 * Slot 31: Close
 * Slots 10-12, 14-16: Upgrade level indicators (4 emeralds = 4 upgrades)
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
        String title = MessageUtil.colorize("&8\u2022 &7" + skillName + " Upgrade &8\u2022");
        createInventory(title, 36);

        // Border + filler
        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(border);

        // ── Tool info (slot 4) ──────────────────────────────────────────────
        Material toolMat = getToolMaterial(skill);
        List<String> toolLore = new ArrayList<>();
        toolLore.add(MessageUtil.colorize(""));
        toolLore.add(MessageUtil.colorize("&7Current Level: &a" + currentUpgrade + "&7/&a4"));
        toolLore.add(MessageUtil.colorize("&7Max Level: &a4"));
        toolLore.add(MessageUtil.colorize(""));
        toolLore.add(MessageUtil.colorize("&7Upgrading your tool allows you to"));
        toolLore.add(MessageUtil.colorize("&7mine higher tier ores."));
        toolLore.add(MessageUtil.colorize(""));
        toolLore.add(MessageUtil.colorize("&7Tier 2 \u2192 Upgrade 1 (500 dust)"));
        toolLore.add(MessageUtil.colorize("&7Tier 3 \u2192 Upgrade 2 (1000 dust)"));
        toolLore.add(MessageUtil.colorize("&7Tier 4 \u2192 Upgrade 3 (1500 dust)"));
        toolLore.add(MessageUtil.colorize("&7Tier 5 \u2192 Upgrade 4 (2000 dust)"));

        setItem(4, new ItemBuilder(toolMat)
                .name(MessageUtil.colorize("&e&l" + skillName + " Tool"))
                .lore(toolLore)
                .hideFlags()
                .build());

        // ── Dust balance (slot 13) ──────────────────────────────────────────
        setItem(13, new ItemBuilder(Material.SUGAR)
                .name(MessageUtil.colorize("&e&lDust Balance"))
                .lore(
                        MessageUtil.colorize(""),
                        MessageUtil.colorize("&7You have: &e" + dust + " &7dust"),
                        MessageUtil.colorize(""),
                        MessageUtil.colorize("&7Earn dust by mining, chopping,"),
                        MessageUtil.colorize("&7farming, and fishing in Wasteland.")
                )
                .build());

        // ── Upgrade level indicators (slots 19-22) ──────────────────────────
        for (int i = 0; i < 4; i++) {
            int level = i + 1;
            boolean unlocked = currentUpgrade >= level;
            Material mat = unlocked ? Material.EMERALD : Material.COAL;
            String name = unlocked ? "&a\u2714 Upgrade " + level : "&8\u2718 Upgrade " + level;
            int upgradeCost = plugin.getDustManager().getUpgradeCost(level);

            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.colorize(""));
            if (unlocked) {
                lore.add(MessageUtil.colorize("&aUnlocked!"));
            } else {
                lore.add(MessageUtil.colorize("&7Cost: &e" + upgradeCost + " dust"));
                lore.add(MessageUtil.colorize("&cNot unlocked yet"));
            }
            lore.add(MessageUtil.colorize(""));
            lore.add(MessageUtil.colorize("&7Required for: &fTier " + (level + 1)));

            setItem(19 + i, new ItemBuilder(mat)
                    .name(MessageUtil.colorize(name))
                    .lore(lore)
                    .build());
        }

        // ── Upgrade button (slot 26) ────────────────────────────────────────
        if (maxed) {
            setItem(26, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&c&lMaxed Out"))
                    .lore(
                            MessageUtil.colorize(""),
                            MessageUtil.colorize("&7This tool is fully upgraded!"),
                            MessageUtil.colorize("&7No further upgrades available.")
                    )
                    .build());
        } else {
            Material buttonMat = canUpgrade ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            String statusColor = canUpgrade ? "&a" : "&c";
            String statusText = canUpgrade ? "Click to upgrade!" : "Not enough dust!";

            List<String> upgradeLore = new ArrayList<>();
            upgradeLore.add(MessageUtil.colorize(""));
            upgradeLore.add(MessageUtil.colorize("&7Next Level: &a" + nextUpgrade));
            upgradeLore.add(MessageUtil.colorize("&7Cost: &e" + cost + " dust"));
            upgradeLore.add(MessageUtil.colorize("&7Your Dust: &e" + dust + " dust"));
            upgradeLore.add(MessageUtil.colorize(""));
            if (canUpgrade) {
                upgradeLore.add(MessageUtil.colorize("&7After upgrade: &e" + (dust - cost) + " dust"));
                upgradeLore.add(MessageUtil.colorize(""));
            }
            upgradeLore.add(MessageUtil.colorize(statusColor + statusText));

            setItem(26, new ItemBuilder(buttonMat)
                    .name(MessageUtil.colorize(statusColor + "&lUpgrade to Level " + nextUpgrade))
                    .lore(upgradeLore)
                    .build());
        }

        // ── Close button (slot 31) ──────────────────────────────────────────
        setItem(31, new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&c&lClose"))
                .lore(MessageUtil.colorize("&7Click to close."))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();

        if (slot == 26) {
            // Try to upgrade.
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (plugin.getDustManager().upgradeTool(data, skill)) {
                plugin.getDataManager().savePlayer(player.getUniqueId());
                player.sendMessage(MessageUtil.colorize("&a&lUpgrade Successful! &7" +
                        skill.getKey().substring(0, 1).toUpperCase() + skill.getKey().substring(1) +
                        " Tool is now Level " + data.getToolUpgradeLevel(skill) + "."));
                player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.5f, 1.5f);
                new DustUpgradeGui(plugin, player, skill).open();
            } else {
                int current = data.getToolUpgradeLevel(skill);
                if (current >= 4) {
                    player.sendMessage(MessageUtil.colorize("&cThis tool is already maxed out!"));
                } else {
                    int cost = plugin.getDustManager().getUpgradeCost(current + 1);
                    player.sendMessage(MessageUtil.colorize("&cYou need &4" + cost + " dust &cto upgrade! " +
                            "You only have &4" + data.getDust() + "&c."));
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.VILLAGER_NO, 0.5f, 1.0f);
            }
        } else if (slot == 31) {
            player.closeInventory();
        }
    }

    private Material getToolMaterial(SkillType skill) {
        switch (skill) {
            case MINING: return Material.DIAMOND_PICKAXE;
            case WOODCUTTING: return Material.DIAMOND_AXE;
            case FARMING: return Material.DIAMOND_HOE;
            case FISHING: return Material.FISHING_ROD;
            default: return Material.STONE;
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
