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
        String title = MessageUtil.colorize("&8" + skillName + " Tool Upgrade");
        createInventory(title, 36);

        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        fill(darkPane);

        Material toolMat = getToolMaterial(skill);
        List<String> toolLore = new ArrayList<>();
        toolLore.add(MessageUtil.colorize(""));
        toolLore.add(MessageUtil.colorize("&7Upgrade Level: &2" + currentUpgrade + "&8/&24"));
        toolLore.add(MessageUtil.colorize(""));
        toolLore.add(MessageUtil.colorize("&7Tier 2 requires Upgrade 1"));
        toolLore.add(MessageUtil.colorize("&7Tier 3 requires Upgrade 2"));
        toolLore.add(MessageUtil.colorize("&7Tier 4 requires Upgrade 3"));
        toolLore.add(MessageUtil.colorize("&7Tier 5 requires Upgrade 4"));

        setItem(4, new ItemBuilder(toolMat)
                .name(MessageUtil.colorize("&2&l" + skillName + " Tool"))
                .lore(toolLore)
                .hideFlags()
                .build());

        setItem(13, new ItemBuilder(Material.SUGAR)
                .name(MessageUtil.colorize("&2&lDust"))
                .lore(
                        MessageUtil.colorize(""),
                        MessageUtil.colorize("&7Balance: &2" + dust)
                )
                .build());

        if (maxed) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&c&lMaxed"))
                    .lore(
                        MessageUtil.colorize(""),
                        MessageUtil.colorize("&7Fully upgraded.")
                    )
                    .build());
        } else {
            Material buttonMat = canUpgrade ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            String statusColor = canUpgrade ? "&2" : "&c";

            List<String> upgradeLore = new ArrayList<>();
            upgradeLore.add(MessageUtil.colorize(""));
            upgradeLore.add(MessageUtil.colorize("&7Next: &2Level " + nextUpgrade));
            upgradeLore.add(MessageUtil.colorize("&7Cost: &2" + cost + " &7dust"));
            upgradeLore.add(MessageUtil.colorize("&7Have: &2" + dust + " &7dust"));
            if (canUpgrade) {
                upgradeLore.add(MessageUtil.colorize("&7After: &2" + (dust - cost) + " &7dust"));
            }
            upgradeLore.add(MessageUtil.colorize(""));
            upgradeLore.add(MessageUtil.colorize(statusColor + (canUpgrade ? "Click to upgrade" : "Not enough dust")));

            setItem(22, new ItemBuilder(buttonMat)
                    .name(MessageUtil.colorize(statusColor + "&lUpgrade"))
                    .lore(upgradeLore)
                    .build());
        }

        setItem(31, new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();

        if (slot == 22) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (plugin.getDustManager().upgradeTool(data, skill)) {
                plugin.getDataManager().savePlayer(player.getUniqueId());
                String skillName = skill.getKey().substring(0, 1).toUpperCase() + skill.getKey().substring(1);
                player.sendMessage(MessageUtil.colorize("&2&lUpgraded! &7" + skillName + " Tool is now Level " +
                        data.getToolUpgradeLevel(skill) + "."));
                player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 0.5f, 1.5f);
                new DustUpgradeGui(plugin, player, skill).open();
            } else {
                int current = data.getToolUpgradeLevel(skill);
                if (current >= 4) {
                    player.sendMessage(MessageUtil.colorize("&cAlready maxed."));
                } else {
                    int c = plugin.getDustManager().getUpgradeCost(current + 1);
                    player.sendMessage(MessageUtil.colorize("&cNeed &4" + c + " dust&c. Have &4" + data.getDust() + "&c."));
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
}
