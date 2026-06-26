package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
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
 * Paginated reward browser for a specific tier.
 * <p>
 * There is only ONE shared tier progression, so this menu does not take
 * a SkillType parameter. Title is simply "Tier N Rewards".
 * <p>
 * Shows ONE showcase item per reward entry. The showcase item is taken
 * verbatim from the reward's {@code display-item} configuration block.
 * Commands are NEVER shown anywhere in this GUI.
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
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        String title = MessageUtil.colorize(cfg.getString("reward-page-menu.title",
                "&7Tier {tier} Rewards")
                .replace("{tier}", String.valueOf(tier)));
        createInventory(title, 54);

        // Border + filler
        ItemStack border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        ItemStack filler = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (short) 7).name(" ").build();
        fill(filler);
        drawBorder(border);

        // Reward slots
        rewardSlots = cfg.getIntegerList("reward-page-menu.reward-slots");
        if (rewardSlots.isEmpty()) {
            for (int i = 10; i <= 34; i++) {
                if (i % 9 != 0 && i % 9 != 8) rewardSlots.add(i);
            }
        }

        List<TierReward> rewards = plugin.getTierManager().getRewards(tier);
        int perPage = rewardSlots.size();
        int start = page * perPage;
        int maxPage = (int) Math.ceil((double) rewards.size() / perPage) - 1;

        boolean claimed = data.hasClaimedTier(tier);
        boolean unlocked = data.getTier() >= tier;

        for (int i = 0; i < perPage && (start + i) < rewards.size(); i++) {
            TierReward reward = rewards.get(start + i);
            int slot = rewardSlots.get(i);

            List<String> lore = new ArrayList<>(reward.getDisplayLore());
            lore.add("");
            String statusLabel;
            if (claimed) {
                statusLabel = MessageUtil.colorize(cfg.getString("reward-page-menu.status.claimed", "&a\u2714 Claimed"));
            } else if (unlocked) {
                statusLabel = MessageUtil.colorize(cfg.getString("reward-page-menu.status.unclaimed", "&e\u25cf Unclaimed"));
            } else {
                statusLabel = MessageUtil.colorize(cfg.getString("reward-page-menu.status.locked", "&c\u2718 Locked"));
            }
            lore.add(statusLabel);

            ItemStack rewardItem = new ItemBuilder(reward.getDisplayMaterial(), 1, reward.getDisplayData())
                    .name(reward.getDisplayName())
                    .lore(lore)
                    .build();
            setItem(slot, rewardItem);
        }

        // Empty-state
        if (rewards.isEmpty()) {
            int emptySlot = 22;
            FileConfiguration msgs = plugin.getConfigManager().getMessages();
            String emptyName = MessageUtil.colorize(msgs.getString("gui.no-rewards", "&7No rewards available."));
            setItem(emptySlot, new ItemBuilder(Material.BARRIER).name(emptyName).build());
        }

        // Prev page
        int prevSlot = cfg.getInt("reward-page-menu.prev-page.slot", 45);
        if (page > 0) {
            Material prevMat = parseMaterial(cfg.getString("reward-page-menu.prev-page.material", "ARROW"), Material.ARROW);
            String prevName = MessageUtil.colorize(cfg.getString("reward-page-menu.prev-page.name", "&7\u00ab Previous Page"));
            setItem(prevSlot, new ItemBuilder(prevMat).name(prevName).build());
        }

        // Next page
        int nextSlot = cfg.getInt("reward-page-menu.next-page.slot", 53);
        int maxPg = (int) Math.ceil((double) rewards.size() / perPage) - 1;
        if (page < maxPg) {
            Material nextMat = parseMaterial(cfg.getString("reward-page-menu.next-page.material", "ARROW"), Material.ARROW);
            String nextName = MessageUtil.colorize(cfg.getString("reward-page-menu.next-page.name", "&aNext Page &7\u00bb"));
            setItem(nextSlot, new ItemBuilder(nextMat).name(nextName).build());
        }

        // Back (returns to tier browser)
        int backSlot = cfg.getInt("reward-page-menu.back.slot", 49);
        Material backMat = parseMaterial(cfg.getString("reward-page-menu.back.material", "ARROW"), Material.ARROW);
        String backName = MessageUtil.colorize(cfg.getString("reward-page-menu.back.name", "&7\u00ab Back"));
        setItem(backSlot, new ItemBuilder(backMat).name(backName).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        List<TierReward> rewards = plugin.getTierManager().getRewards(tier);
        int perPage = rewardSlots != null ? rewardSlots.size() : 21;
        int maxPage = (int) Math.ceil((double) rewards.size() / perPage) - 1;

        int backSlot = cfg.getInt("reward-page-menu.back.slot", 49);
        int prevSlot = cfg.getInt("reward-page-menu.prev-page.slot", 45);
        int nextSlot = cfg.getInt("reward-page-menu.next-page.slot", 53);

        if (slot == backSlot) {
            new TierMenuGui(plugin, player).open();
        } else if (slot == prevSlot && page > 0) {
            new RewardPageMenuGui(plugin, player, tier, page - 1).open();
        } else if (slot == nextSlot && page < maxPage) {
            new RewardPageMenuGui(plugin, player, tier, page + 1).open();
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
}
