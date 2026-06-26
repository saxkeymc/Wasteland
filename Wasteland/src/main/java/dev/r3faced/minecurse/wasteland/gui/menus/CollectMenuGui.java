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
 * Virtual reward backpack — a paginated GUI showing ALL of the player's
 * stored rewards from every tier, merged into one continuous storage.
 * <p>
 * Design (Adventure Backpack style):
 * <ul>
 *   <li>54-slot inventory per page.</li>
 *   <li>45 reward slots per page (rows 1-5, excluding the border).</li>
 *   <li>Rewards with identical display items (material + data + name)
 *       stack visually into a single slot with an item-count.</li>
 *   <li>Bottom row holds Prev-Page, Close, and Next-Page buttons.</li>
 *   <li>Pages are created automatically as the player accumulates rewards.</li>
 *   <li>The page the player was viewing is remembered via the page field.</li>
 * </ul>
 * <p>
 * Clicking a reward claims ONE unit from the stack (executes the hidden
 * commands for that one reward and decrements the visual stack). The
 * actual commands execute individually even when stacked.
 * <p>
 * The "Close" button returns the player to the Main Wasteland GUI.
 */
public class CollectMenuGui extends WastelandGui {

    private int page;

    /**
     * Snapshot of the stacked rewards displayed on the current page.
     * Built in build() and read in handleClick(). Each entry is a
     * (display-item, list-of-underlying-stored-rewards) pair so that
     * clicking can claim exactly one reward from the stack.
     */
    private final List<StackedEntry> pageEntries = new ArrayList<>();

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
        createInventory(title, 54);

        // Border + filler
        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(border);

        // Build the stacked view of ALL stored rewards (every tier merged).
        List<StackedEntry> allStacked = buildStackedView();

        // Pagination — 45 slots per page (rows 1-5, slots 10-44 excluding border).
        List<Integer> rewardSlots = cfg.getIntegerList("collect-menu.reward-slots");
        if (rewardSlots.isEmpty()) {
            for (int i = 10; i <= 44; i++) {
                if (i % 9 != 0 && i % 9 != 8) rewardSlots.add(i);
            }
        }
        int perPage = rewardSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) allStacked.size() / perPage));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int start = page * perPage;
        pageEntries.clear();
        for (int i = 0; i < perPage && (start + i) < allStacked.size(); i++) {
            StackedEntry entry = allStacked.get(start + i);
            pageEntries.add(entry);
            setItem(rewardSlots.get(i), entry.toItemStack(cfg));
        }

        // Empty-state message
        if (allStacked.isEmpty()) {
            int emptySlot = 22;
            FileConfiguration msgs = plugin.getConfigManager().getMessages();
            String emptyName = MessageUtil.colorize(msgs.getString("gui.no-rewards", "&7No rewards available."));
            setItem(emptySlot, new ItemBuilder(Material.BARRIER).name(emptyName).build());
        }

        // ── Navigation buttons (bottom row) ──────────────────────────────────
        int prevSlot = cfg.getInt("collect-menu.prev-page.slot", 45);
        int nextSlot = cfg.getInt("collect-menu.next-page.slot", 53);
        int closeSlot = cfg.getInt("collect-menu.close.slot", 49);
        int pageIndicatorSlot = cfg.getInt("collect-menu.page-indicator.slot", 48);

        if (page > 0) {
            Material prevMat = parseMaterial(cfg.getString("collect-menu.prev-page.material", "ARROW"), Material.ARROW);
            int prevData = cfg.getInt("collect-menu.prev-page.data", 0);
            String prevName = cfg.getString("collect-menu.prev-page.name", "&7\u00ab Previous Page");
            List<String> prevLore = MessageUtil.colorizeList(cfg.getStringList("collect-menu.prev-page.lore"));
            setItem(prevSlot, new ItemBuilder(prevMat, 1, (short) prevData).name(prevName).lore(prevLore).build());
        }

        if (page < totalPages - 1) {
            Material nextMat = parseMaterial(cfg.getString("collect-menu.next-page.material", "ARROW"), Material.ARROW);
            int nextData = cfg.getInt("collect-menu.next-page.data", 0);
            String nextName = cfg.getString("collect-menu.next-page.name", "&aNext Page &7\u00bb");
            List<String> nextLore = MessageUtil.colorizeList(cfg.getStringList("collect-menu.next-page.lore"));
            setItem(nextSlot, new ItemBuilder(nextMat, 1, (short) nextData).name(nextName).lore(nextLore).build());
        }

        // Page indicator (e.g. "Page 1/3")
        Material indicatorMat = parseMaterial(cfg.getString("collect-menu.page-indicator.material", "PAPER"), Material.PAPER);
        int indicatorData = cfg.getInt("collect-menu.page-indicator.data", 0);
        String indicatorName = cfg.getString("collect-menu.page-indicator.name", "&7Page &f{page}&7/&f{max}");
        indicatorName = MessageUtil.colorize(indicatorName
                .replace("{page}", String.valueOf(page + 1))
                .replace("{max}", String.valueOf(totalPages)));
        List<String> indicatorLore = MessageUtil.colorizeList(cfg.getStringList("collect-menu.page-indicator.lore"));
        setItem(pageIndicatorSlot, new ItemBuilder(indicatorMat, 1, (short) indicatorData).name(indicatorName).lore(indicatorLore).build());

        // Close button — returns to Main Wasteland GUI.
        Material closeMat = parseMaterial(cfg.getString("collect-menu.close.material", "BARRIER"), Material.BARRIER);
        int closeData = cfg.getInt("collect-menu.close.data", 0);
        String closeName = cfg.getString("collect-menu.close.name", "&c&lClose");
        List<String> closeLore = MessageUtil.colorizeList(cfg.getStringList("collect-menu.close.lore"));
        setItem(closeSlot, new ItemBuilder(closeMat, 1, (short) closeData).name(closeName).lore(closeLore).build());
    }

    /**
     * Build the stacked view of all stored rewards. Rewards with the same
     * display identity (material + data + name) are merged into a single
     * StackedEntry. The underlying StoredReward instances are preserved so
     * clicking can claim them one at a time.
     */
    private List<StackedEntry> buildStackedView() {
        List<StoredReward> rewards = plugin.getDataManager().getPlayerData(player.getUniqueId()).getStoredRewards();
        // Use LinkedHashMap to preserve insertion order while deduplicating.
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

    /** Build a stable key for stacking: material + ":" + data + ":" + name. */
    private String stackKey(StoredReward r) {
        return r.getDisplayMaterial().name() + ":" + r.getDisplayData() + ":" + (r.getDisplayName() == null ? "" : r.getDisplayName());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        int closeSlot = cfg.getInt("collect-menu.close.slot", 49);
        int prevSlot  = cfg.getInt("collect-menu.prev-page.slot", 45);
        int nextSlot  = cfg.getInt("collect-menu.next-page.slot", 53);

        // Close → return to Main Wasteland GUI
        if (slot == closeSlot) {
            new MainMenuGui(plugin, player).open();
            return;
        }
        // Previous page
        if (slot == prevSlot) {
            if (page > 0) new CollectMenuGui(plugin, player, page - 1).open();
            return;
        }
        // Next page
        if (slot == nextSlot) {
            new CollectMenuGui(plugin, player, page + 1).open();
            return;
        }

        // Reward slots — claim one unit from the clicked stack.
        List<Integer> rewardSlots = cfg.getIntegerList("collect-menu.reward-slots");
        if (rewardSlots.isEmpty()) {
            for (int i = 10; i <= 44; i++) {
                if (i % 9 != 0 && i % 9 != 8) rewardSlots.add(i);
            }
        }
        int localIndex = rewardSlots.indexOf(slot);
        if (localIndex >= 0 && localIndex < pageEntries.size()) {
            StackedEntry entry = pageEntries.get(localIndex);
            if (entry != null && !entry.getRewards().isEmpty()) {
                // Claim ONE reward from the stack.
                StoredReward toClaim = entry.getRewards().get(0);
                plugin.getTierManager().claimStoredReward(player, toClaim);
                // Refresh the menu to reflect the new state.
                new CollectMenuGui(plugin, player, page).open();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
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

    /**
     * A stacked entry in the GUI — one or more StoredRewards that share the
     * same display identity. Renders as a single ItemStack with an item-count
     * equal to the number of underlying rewards. Clicking claims ONE reward
     * from the stack (executes its commands, removes it from storage).
     */
    private static class StackedEntry {
        private final List<StoredReward> rewards = new ArrayList<>();
        private final StoredReward template;

        StackedEntry(StoredReward first) {
            this.template = first;
            this.rewards.add(first);
        }

        void add(StoredReward r) {
            rewards.add(r);
        }

        List<StoredReward> getRewards() {
            return rewards;
        }

        ItemStack toItemStack(FileConfiguration cfg) {
            int count = Math.min(rewards.size(), 64);
            ItemStack item = new ItemBuilder(template.getDisplayMaterial(), count, template.getDisplayData())
                    .name(template.getDisplayName())
                    .lore(template.getDisplayLore())
                    .build();

            // Append stacking info to the lore if configured.
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
                // Add a blank line + count line + click-to-claim line.
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
