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
 * Wasteland Backpack GUI — clean, dark, minimal design.
 * <p>
 * Shows items collected from player kills. Click an item to take it.
 * Dynamic sizing based on item count. Paginated if needed.
 * <p>
 * Design:
 * - Dark glass border (data 15)
 * - Light glass filler (data 7)
 * - Items fill left-to-right starting at slot 0
 * - No decorative items, no clutter
 * - Navigation only when needed
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

        // Dynamic sizing.
        int rows;
        if (hasMultiPages) {
            rows = 6; // 5 item rows + 1 nav row
        } else {
            int itemCount = Math.min(total, MAX_PER_PAGE);
            rows = Math.max(1, (int) Math.ceil(itemCount / 9.0));
            if (rows < 1) rows = 1;
        }
        int size = rows * 9;

        String title = MessageUtil.colorize("&8Backpack");
        createInventory(title, size);

        // Fill with dark glass for a clean look.
        ItemStack darkPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        fill(darkPane);

        // Place items starting at slot 0, left-to-right.
        int start = page * MAX_PER_PAGE;
        int count = Math.min(MAX_PER_PAGE, total - start);
        for (int i = 0; i < count; i++) {
            setItem(i, items.get(start + i).clone());
        }

        // Empty state.
        if (total == 0) {
            setItem(4, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&7Backpack is empty."))
                    .lore(MessageUtil.colorize("&7Kill players to collect loot."))
                    .build());
        }

        // Navigation row (only if multiple pages).
        if (hasMultiPages) {
            // Previous.
            if (page > 0) {
                setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&7Previous Page"))
                        .build());
            } else {
                setItem(45, darkPane);
            }

            // Close.
            setItem(49, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&cClose"))
                    .build());

            // Next.
            if (page < totalPages - 1) {
                setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&aNext Page"))
                        .build());
            } else {
                setItem(53, darkPane);
            }

            // Page indicator.
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
            if (slot == 49 || slot == 48) {
                event.setCancelled(true);
                if (slot == 49) player.closeInventory();
                return;
            }

            // Don't allow taking glass panes, barriers, arrows, paper.
            if (clicked.getType() == Material.STAINED_GLASS_PANE ||
                clicked.getType() == Material.BARRIER ||
                clicked.getType() == Material.ARROW ||
                clicked.getType() == Material.PAPER) {
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
