package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.managers.TierManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.TierReward;
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
 * Shows all claimable (unlocked but unclaimed) tier rewards.
 * <p>
 * The tier system is SHARED across all skills — there is only ONE tier
 * progression per player. Each claimable tier is rendered as a single
 * showcase item taken from the FIRST reward in that tier's reward list.
 * Commands are NEVER shown anywhere in this GUI.
 * <p>
 * Clicking an item claims ALL rewards for that tier, rolling each
 * reward's chance independently.
 * <p>
 * The "Close" button returns the player to the Main Wasteland GUI.
 */
public class CollectMenuGui extends WastelandGui {

    private int page;

    /** Ordered list of tier numbers that are claimable. */
    private final List<Integer> claimable = new ArrayList<>();

    public CollectMenuGui(WastelandPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public CollectMenuGui(WastelandPlugin plugin, Player player, int page) {
        super(plugin, player);
        this.page = page;
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

        // Build claimable list (single shared tier progression)
        claimable.clear();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        for (int tier = 1; tier <= TierManager.TIER_COUNT; tier++) {
            if (data.getTier() >= tier && !data.hasClaimedTier(tier)) {
                claimable.add(tier);
            }
        }

        List<Integer> rewardSlots = cfg.getIntegerList("collect-menu.reward-slots");
        if (rewardSlots.isEmpty()) {
            for (int i = 10; i <= 34; i++) {
                if (i % 9 != 0 && i % 9 != 8) rewardSlots.add(i);
            }
        }

        int perPage = rewardSlots.size();
        int start = page * perPage;

        for (int i = 0; i < perPage && (start + i) < claimable.size(); i++) {
            int tier = claimable.get(start + i);
            ItemStack showcase = buildShowcaseItem(cfg, tier);
            setItem(rewardSlots.get(i), showcase);
        }

        // Empty-state message
        if (claimable.isEmpty()) {
            int emptySlot = 22;
            FileConfiguration msgs = plugin.getConfigManager().getMessages();
            String emptyName = MessageUtil.colorize(msgs.getString("gui.no-rewards", "&7No rewards available."));
            setItem(emptySlot, new ItemBuilder(Material.BARRIER).name(emptyName).build());
        }

        // Navigation
        int prevSlot = cfg.getInt("collect-menu.prev-page.slot", 45);
        int nextSlot = cfg.getInt("collect-menu.next-page.slot", 53);
        int closeSlot = cfg.getInt("collect-menu.close.slot", 49);
        int maxPage = (int) Math.ceil((double) claimable.size() / perPage) - 1;

        if (page > 0) {
            setItem(prevSlot, new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.colorize(cfg.getString("collect-menu.prev-page.name", "&7\u00ab Previous Page"))).build());
        }
        if (page < maxPage) {
            setItem(nextSlot, new ItemBuilder(Material.ARROW)
                    .name(MessageUtil.colorize(cfg.getString("collect-menu.next-page.name", "&aNext Page &7\u00bb"))).build());
        }
        setItem(closeSlot, new ItemBuilder(Material.BARRIER)
                .name(MessageUtil.colorize(cfg.getString("collect-menu.close.name", "&c&lClose")))
                .lore(MessageUtil.colorizeList(cfg.getStringList("collect-menu.close.lore")))
                .build());
    }

    /**
     * Build the showcase ItemStack for a tier.
     * Uses the FIRST reward's display-item, augmented with a tier label.
     * Command rewards are NEVER mentioned in the lore.
     */
    private ItemStack buildShowcaseItem(FileConfiguration cfg, int tier) {
        List<TierReward> rewards = plugin.getTierManager().getRewards(tier);

        if (!rewards.isEmpty()) {
            TierReward first = rewards.get(0);
            List<String> lore = new ArrayList<>(first.getDisplayLore());
            lore.add("");
            lore.add(MessageUtil.colorize("&fTier: &a" + tier));
            lore.add("");
            lore.add(MessageUtil.colorize("&e\u25cf Click to claim!"));
            return new ItemBuilder(first.getDisplayMaterial(), 1, first.getDisplayData())
                    .name(first.getDisplayName())
                    .lore(lore)
                    .build();
        }

        // Fallback if no rewards configured for this tier
        return new ItemBuilder(Material.CHEST)
                .name("&eTier " + tier + " Reward")
                .lore(
                        MessageUtil.colorize("&7Click to claim this reward!"),
                        "",
                        MessageUtil.colorize("&fTier: &a" + tier)
                )
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        List<Integer> rewardSlots = cfg.getIntegerList("collect-menu.reward-slots");
        if (rewardSlots.isEmpty()) {
            for (int i = 10; i <= 34; i++) {
                if (i % 9 != 0 && i % 9 != 8) rewardSlots.add(i);
            }
        }

        int perPage = rewardSlots.size();
        int closeSlot = cfg.getInt("collect-menu.close.slot", 49);
        int prevSlot  = cfg.getInt("collect-menu.prev-page.slot", 45);
        int nextSlot  = cfg.getInt("collect-menu.next-page.slot", 53);

        if (slot == closeSlot) {
            new MainMenuGui(plugin, player).open();
            return;
        }
        if (slot == prevSlot) {
            if (page > 0) new CollectMenuGui(plugin, player, page - 1).open();
            return;
        }
        if (slot == nextSlot) {
            new CollectMenuGui(plugin, player, page + 1).open();
            return;
        }

        int localIndex = rewardSlots.indexOf(slot);
        if (localIndex >= 0) {
            int globalIndex = page * perPage + localIndex;
            if (globalIndex < claimable.size()) {
                int tier = claimable.get(globalIndex);
                plugin.getTierManager().dispatchRewards(player, tier);
                new CollectMenuGui(plugin, player, page).open();
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
