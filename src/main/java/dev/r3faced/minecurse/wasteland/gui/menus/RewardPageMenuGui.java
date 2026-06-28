package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.TierReward;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reward preview GUI for a specific tier.
 * <p>
 * Dynamic sizing based on reward count:
 * <ul>
 *   <li>1-9 rewards → 1 row (9 slots). Close button at slot 8 (last slot).</li>
 *   <li>10-18 rewards → 2 rows (18 slots). Close button at slot 17.</li>
 *   <li>19-27 rewards → 3 rows (27 slots). Close button at slot 26.</li>
 *   <li>...up to 5 rows (45 slots). If more than 45 rewards, pagination kicks in.</li>
 * </ul>
 * <p>
 * For 4+ rows, the close button is placed in the bottom-middle slot.
 * For 1-3 rows, the close button is at the last slot of the last row.
 * <p>
 * No messages are sent when clicking. Players can view ANY tier's
 * rewards regardless of whether they've unlocked it.
 * <p>
 * Commands are NEVER shown — only the display items.
 */
public class RewardPageMenuGui extends WastelandGui {

    private final int tier;
    private final int page;
    private List<Integer> rewardSlots;

    public RewardPageMenuGui(WastelandPlugin plugin, Player player, int tier, int page) {
        super(plugin, player);
        this.tier  = tier;
        this.page  = page;
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        FileConfiguration tiersCfg = plugin.getConfigManager().getTiers();
        String displayName = tiersCfg.getString("tiers." + tier + ".display-name", "&fTier " + tier);

        List<TierReward> rewards = plugin.getTierManager().getRewards(tier);
        int totalRewards = rewards.size();

        // Calculate pagination — max 45 rewards per page (5 rows × 9).
        int maxPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRewards / maxPerPage));
        int currentPage = Math.min(page, totalPages - 1);
        if (currentPage < 0) currentPage = 0;

        int start = currentPage * maxPerPage;
        int rewardsThisPage = Math.min(maxPerPage, totalRewards - start);

        // Calculate dynamic inventory size.
        int rows;
        boolean needsNavRow = totalPages > 1;
        if (needsNavRow) {
            // With pagination: 5 reward rows + 1 nav row = 6 rows (54 slots)
            rows = 6;
        } else {
            // Without pagination: rows = ceil(rewardsThisPage / 9), min 1
            rows = Math.max(1, (int) Math.ceil(rewardsThisPage / 9.0));
            // If there are 0 rewards, still show 1 row with a close button.
            if (rows == 0) rows = 1;
        }
        int size = rows * 9;

        // Title: just the tier display name + " Rewards"
        String title = MessageUtil.colorize(displayName + " &7Rewards");
        createInventory(title, size);

        // Fill reward slots — start at slot 0, fill left-to-right.
        rewardSlots = new ArrayList<>();
        for (int i = 0; i < rewardsThisPage && (start + i) < rewards.size(); i++) {
            TierReward reward = rewards.get(start + i);
            int slot = i;
            rewardSlots.add(slot);

            // Build the display item with enchants + flags.
            List<String> lore = new ArrayList<>(reward.getDisplayLore());
            ItemStack rewardItem = new ItemBuilder(reward.getDisplayMaterial(), 1, reward.getDisplayData())
                    .name(reward.getDisplayName())
                    .lore(lore)
                    .build();

            // Apply enchantments and item flags.
            ItemMeta meta = rewardItem.getItemMeta();
            if (meta != null) {
                if (reward.getDisplayEnchants() != null) {
                    for (Map.Entry<String, Integer> ench : reward.getDisplayEnchants().entrySet()) {
                        try {
                            org.bukkit.enchantments.Enchantment enchant =
                                    org.bukkit.enchantments.Enchantment.getByName(ench.getKey());
                            if (enchant != null) {
                                meta.addEnchant(enchant, ench.getValue(), true);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (reward.getDisplayItemFlags() != null) {
                    for (String flagName : reward.getDisplayItemFlags()) {
                        try {
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flagName));
                        } catch (Exception ignored) {}
                    }
                }
                rewardItem.setItemMeta(meta);
            }
            setItem(slot, rewardItem);
        }

        // ── Close button ─────────────────────────────────────────────────────
        // For 1-3 rows: close button at the last slot of the last row.
        // For 4+ rows: close button at the bottom-middle slot.
        // For 6 rows (pagination): close button at slot 49 (bottom-middle of nav row).
        int closeSlot;
        if (needsNavRow) {
            closeSlot = 49; // Bottom-middle of the 6th row
        } else if (rows >= 4) {
            closeSlot = (rows * 9) - 5; // Bottom-middle slot
        } else {
            closeSlot = (rows * 9) - 1; // Last slot of last row
        }

        setItem(closeSlot, new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());

        // ── Navigation buttons (only if pagination is needed) ────────────────
        if (needsNavRow) {
            // Previous page button (slot 45)
            if (currentPage > 0) {
                setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&7\u00ab Previous Page"))
                        .build());
            }
            // Next page button (slot 53)
            if (currentPage < totalPages - 1) {
                setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&aNext Page &7\u00bb"))
                        .build());
            }
        }

        // Empty state
        if (totalRewards == 0) {
            setItem(0, new ItemBuilder(Material.BARRIER)
                    .name(MessageUtil.colorize("&7No rewards configured for this tier."))
                    .build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        int slot = event.getSlot();
        int size = inventory.getSize();
        int rows = size / 9;

        // Check if this is a navigation click — no message sent.
        boolean needsNavRow = rows == 6;
        if (needsNavRow) {
            if (slot == 45 && page > 0) {
                new RewardPageMenuGui(plugin, player, tier, page - 1).open();
                return;
            }
            if (slot == 53) {
                new RewardPageMenuGui(plugin, player, tier, page + 1).open();
                return;
            }
            if (slot == 49) {
                // Close button → go back to tier menu
                new TierMenuGui(plugin, player).open();
                return;
            }
        } else {
            // Non-paginated: check close button position
            int closeSlot;
            if (rows >= 4) {
                closeSlot = (rows * 9) - 5;
            } else {
                closeSlot = (rows * 9) - 1;
            }
            if (slot == closeSlot) {
                new TierMenuGui(plugin, player).open();
                return;
            }
        }

        // Reward slots — no action (preview only, no claiming here).
        // This GUI is just for viewing rewards, not claiming them.
    }
}
