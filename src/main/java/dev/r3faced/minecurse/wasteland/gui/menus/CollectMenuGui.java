package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.StoredReward;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual reward backpack — a dynamically-sized, paginated GUI showing
 * ALL of the player's stored rewards from every tier, merged into one
 * continuous storage.
 * <p>
 * Design (Adventure Backpack style):
 * <ul>
 *   <li>Inventory size is dynamic: only as many rows as needed.</li>
 *   <li>1–9 rewards → 1 row, 10–18 → 2 rows, ... up to 5 rows (45 slots).</li>
 *   <li>If more than 45 rewards exist, pages are created automatically.</li>
 *   <li>When multiple pages exist, a 6th row is added for navigation.</li>
 *   <li>Rewards start in the top-left slot and fill left-to-right.</li>
 *   <li>No filler glass between rewards — clean and compact.</li>
 *   <li>Rewards with identical display items stack visually.</li>
 *   <li>Navigation buttons only appear when another page exists.</li>
 * </ul>
 * <p>
 * Clicking a reward claims ONE unit from the stack.
 */
public class CollectMenuGui extends WastelandGui {

    private int page;

    /** Snapshot of stacked rewards on the current page. */
    private final List<StackedEntry> pageEntries = new ArrayList<>();

    /** Max rewards per page (5 rows × 9 cols = 45). */
    private static final int MAX_PER_PAGE = 45;

    public CollectMenuGui(WastelandPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public CollectMenuGui(WastelandPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = Math.max(0, page);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        String title = MessageUtil.colorize(cfg.getString("collect-menu.title",
                "&d&7Collect Rewards"));

        // Build the stacked view of ALL stored rewards.
        List<StackedEntry> allStacked = buildStackedView();
        int totalRewards = allStacked.size();

        // Calculate pagination.
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRewards / MAX_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        boolean hasMultiplePages = totalPages > 1;

        // Determine how many rewards are on THIS page.
        int start = page * MAX_PER_PAGE;
        int rewardsThisPage = Math.min(MAX_PER_PAGE, totalRewards - start);

        // Calculate dynamic inventory size:
        // - Single page: rows = ceil(rewardsThisPage / 9), min 1.
        // - Multiple pages: 6 rows (5 reward rows + 1 nav row).
        int rows;
        if (hasMultiplePages) {
            rows = 6; // 5 reward rows (45 slots) + 1 nav row
        } else {
            rows = Math.max(1, (int) Math.ceil(rewardsThisPage / 9.0));
            // Cap at 6 rows max.
            rows = Math.min(rows, 6);
        }
        int size = rows * 9;

        // Create inventory with dynamic size.
        createInventory(title, size);

        // Fill reward slots — no filler glass, just rewards starting top-left.
        pageEntries.clear();
        for (int i = 0; i < rewardsThisPage && (start + i) < allStacked.size(); i++) {
            StackedEntry entry = allStacked.get(start + i);
            pageEntries.add(entry);
            setItem(i, entry.toItemStack(cfg));
        }

        // Empty-state message
        if (totalRewards == 0) {
            FileConfiguration msgs = plugin.getConfigManager().getMessages();
            String emptyName = MessageUtil.colorize(msgs.getString("gui.no-rewards", "&7No rewards available."));
            setItem(0, new ItemBuilder(Material.BARRIER).name(emptyName).build());
        }

        // ── Navigation buttons (only if multiple pages) ─────────────────────
        if (hasMultiplePages) {
            int navRow = 5; // 6th row (0-indexed: row 5, slots 45-53)
            int prevSlot = navRow * 9 + 0;       // slot 45
            int nextSlot = navRow * 9 + 8;       // slot 53
            int closeSlot = navRow * 9 + 4;      // slot 49
            int pageIndicatorSlot = navRow * 9 + 3; // slot 48

            // Previous page (only if not on first page)
            if (page > 0) {
                setItem(prevSlot, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize(cfg.getString("collect-menu.prev-page.name", "&7\u00ab Previous Page")))
                        .build());
            }

            // Next page (only if not on last page)
            if (page < totalPages - 1) {
                setItem(nextSlot, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize(cfg.getString("collect-menu.next-page.name", "&aNext Page &7\u00bb")))
                        .build());
            }

            // Page indicator
            String indicatorName = MessageUtil.colorize(
                    cfg.getString("collect-menu.page-indicator.name", "&7Page &f{page}&7/&f{max}")
                            .replace("{page}", String.valueOf(page + 1))
                            .replace("{max}", String.valueOf(totalPages)));
            setItem(pageIndicatorSlot, new ItemBuilder(Material.PAPER).name(indicatorName).build());

            // Close button
            setItem(closeSlot, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize(cfg.getString("collect-menu.close.name", "&c&lClose")))
                    .build());
        }
    }

    private List<StackedEntry> buildStackedView() {
        List<StoredReward> rewards = plugin.getDataManager().getPlayerData(player.getUniqueId()).getStoredRewards();
        Map<String, StackedEntry> bucket = new LinkedHashMap<>();
        for (StoredReward r : rewards) {
            String key = stackKey(r);
            StackedEntry entry = bucket.get(key);
            if (entry == null) {
                entry = new StackedEntry(r);
                bucket.put(key, entry);
            } else {
                entry.add(r);
            }
        }
        return new ArrayList<>(bucket.values());
    }

    private String stackKey(StoredReward r) {
        return r.getDisplayMaterial().name() + ":" + r.getDisplayData() + ":" + (r.getDisplayName() == null ? "" : r.getDisplayName());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        int totalRewards = plugin.getDataManager().getPlayerData(player.getUniqueId()).getStoredRewards().size();
        int totalPages = Math.max(1, (int) Math.ceil(totalRewards / 45.0));
        boolean hasMultiplePages = totalPages > 1;

        if (hasMultiplePages) {
            int navRow = 5;
            int prevSlot = navRow * 9 + 0;
            int nextSlot = navRow * 9 + 8;
            int closeSlot = navRow * 9 + 4;

            if (slot == closeSlot) {
                new MainMenuGui(plugin, player).open();
                return;
            }
            if (slot == prevSlot && page > 0) {
                new CollectMenuGui(plugin, player, page - 1).open();
                return;
            }
            if (slot == nextSlot && page < totalPages - 1) {
                new CollectMenuGui(plugin, player, page + 1).open();
                return;
            }
        }

        // Reward slots — claim one unit from the clicked stack.
        if (slot < pageEntries.size()) {
            StackedEntry entry = pageEntries.get(slot);
            if (entry != null && !entry.getRewards().isEmpty()) {
                StoredReward toClaim = entry.getRewards().get(0);
                plugin.getTierManager().claimStoredReward(player, toClaim);
                new CollectMenuGui(plugin, player, page).open();
            }
        }
    }

    // ── Stacked Entry ─────────────────────────────────────────────────────────

    private static class StackedEntry {
        private final List<StoredReward> rewards = new ArrayList<>();
        private final StoredReward template;

        StackedEntry(StoredReward first) {
            this.template = first;
            this.rewards.add(first);
        }

        void add(StoredReward r) { rewards.add(r); }
        List<StoredReward> getRewards() { return rewards; }

        ItemStack toItemStack(FileConfiguration cfg) {
            int count = Math.min(rewards.size(), 64);
            ItemStack item = new ItemBuilder(template.getDisplayMaterial(), count, template.getDisplayData())
                    .name(template.getDisplayName())
                    .lore(template.getDisplayLore())
                    .build();

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
                String countLine = MessageUtil.colorize(
                        cfg.getString("collect-menu.stacking.count-line", "&7Amount: &f{count}")
                                .replace("{count}", String.valueOf(rewards.size())));
                String claimLine = MessageUtil.colorize(
                        cfg.getString("collect-menu.stacking.claim-line", "&eClick to claim one."));
                lore.add("");
                lore.add(countLine);
                lore.add(claimLine);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }
    }
}
