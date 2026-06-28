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

/**
 * Wasteland Backpack GUI — shows items collected from player kills.
 * Click an item to take it. Paginated if more than 45 items.
 */
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
        int rows = hasMultiPages ? 6 : Math.max(1, (int) Math.ceil(Math.min(total, MAX_PER_PAGE) / 9.0));
        int size = rows * 9;

        String title = MessageUtil.colorize("&8Backpack");
        createInventory(title, size);

        int start = page * MAX_PER_PAGE;
        int count = Math.min(MAX_PER_PAGE, total - start);
        for (int i = 0; i < count; i++) {
            setItem(i, items.get(start + i).clone());
        }

        if (total == 0) {
            setItem(0, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&7Backpack is empty."))
                    .build());
        }

        if (hasMultiPages) {
            if (page > 0) {
                setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&7Previous"))
                        .build());
            }
            if (page < totalPages - 1) {
                setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&aNext"))
                        .build());
            }
            setItem(49, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&cClose"))
                    .build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        // This GUI is interactive — players can take items out.
        int slot = event.getRawSlot();
        int size = inventory.getSize();

        if (slot >= 0 && slot < size) {
            // Clicked inside our inventory.
            ItemStack clicked = inventory.getItem(slot);
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Navigation buttons.
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
            if (slot == 49) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }

            // Barrier (empty state) — don't allow taking.
            if (clicked.getType() == Material.BARRIER || clicked.getType() == Material.ARROW) {
                event.setCancelled(true);
                return;
            }

            // Allow taking the item — remove from backpack.
            List<ItemStack> items = plugin.getDataManager().getPlayerData(player.getUniqueId()).getBackpackItems();
            int start = page * MAX_PER_PAGE;
            int index = start + slot;
            if (index < items.size()) {
                items.remove(index);
                plugin.getDataManager().savePlayer(player.getUniqueId());
                // Don't cancel — let the item move to the player's inventory.
                // Refresh after 1 tick.
                final int currentPage = page;
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        new BackpackMenuGui(plugin, player, currentPage).open();
                    }
                }, 1L);
            }
        }
        // Clicks in player's own inventory are allowed (shift-click to deposit).
    }
}
