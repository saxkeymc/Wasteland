package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BackpackMenuGui extends WastelandGui {

    private int page;
    private static final int MAX_PER_PAGE = 45;

    public BackpackMenuGui(WastelandPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public BackpackMenuGui(WastelandPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = Math.max(0, page);
    }

    @Override
    public void build() {
        List<ItemStack> items = plugin.getDataManager().getPlayerData(player.getUniqueId()).getBackpackItems();
        int total = items.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / MAX_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        boolean hasMultiPages = totalPages > 1;

        int rows;
        if (hasMultiPages) {
            rows = 6;
        } else {
            int itemCount = Math.min(total, MAX_PER_PAGE);
            rows = Math.max(1, (int) Math.ceil(itemCount / 9.0));
            if (rows < 1) rows = 1;
        }
        int size = rows * 9;

        String title = MessageUtil.colorize("&8Backpack");
        createInventory(title, size);

        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        fill(darkPane);

        int start = page * MAX_PER_PAGE;
        int count = Math.min(MAX_PER_PAGE, total - start);
        for (int i = 0; i < count; i++) {
            setItem(i, items.get(start + i).clone());
        }

        if (total == 0) {
            setItem(4, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&7Backpack is empty."))
                    .lore(MessageUtil.colorize("&7Kill players to collect loot."))
                    .build());
        }

        if (hasMultiPages) {
            if (page > 0) {
                setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&7Previous Page"))
                        .build());
            } else {
                setItem(45, darkPane);
            }

            setItem(49, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&cClose"))
                    .build());

            if (page < totalPages - 1) {
                setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&aNext Page"))
                        .build());
            } else {
                setItem(53, darkPane);
            }

            String pageText = "&7" + (page + 1) + "&8/&7" + totalPages;
            setItem(48, new ItemBuilder(Material.PAPER)
                    .name(MessageUtil.colorize(pageText))
                    .build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int size = inventory.getSize();

        if (slot >= 0 && slot < size) {
            ItemStack clicked = inventory.getItem(slot);
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (slot == 45 && page > 0) {
                event.setCancelled(true);
                new BackpackMenuGui(plugin, player, page - 1).open();
                return;
            }
            if (slot == 53) {
                event.setCancelled(true);
                new BackpackMenuGui(plugin, player, page + 1).open();
                return;
            }
            if (slot == 49 || slot == 48) {
                event.setCancelled(true);
                if (slot == 49) player.closeInventory();
                return;
            }

            if (clicked.getType() == Material.STAINED_GLASS_PANE ||
                clicked.getType() == Material.BARRIER ||
                clicked.getType() == Material.ARROW ||
                clicked.getType() == Material.PAPER) {
                event.setCancelled(true);
                return;
            }

            List<ItemStack> items = plugin.getDataManager().getPlayerData(player.getUniqueId()).getBackpackItems();
            int start = page * MAX_PER_PAGE;
            int index = start + slot;
            if (index < items.size()) {
                items.remove(index);
                plugin.getDataManager().savePlayer(player.getUniqueId());
                final int currentPage = page;
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        new BackpackMenuGui(plugin, player, currentPage).open();
                    }
                }, 1L);
            }
        }
    }
}
