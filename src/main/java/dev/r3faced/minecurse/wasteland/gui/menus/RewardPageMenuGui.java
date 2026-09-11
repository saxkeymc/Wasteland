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

        int maxPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRewards / maxPerPage));
        int currentPage = Math.min(page, totalPages - 1);
        if (currentPage < 0) currentPage = 0;

        int start = currentPage * maxPerPage;
        int rewardsThisPage = Math.min(maxPerPage, totalRewards - start);

        int rows;
        boolean needsNavRow = totalPages > 1;
        if (needsNavRow) {
            rows = 6;
        } else {
            rows = Math.max(1, (int) Math.ceil(rewardsThisPage / 9.0));
            if (rows == 0) rows = 1;
        }
        int size = rows * 9;

        String title = MessageUtil.colorize(displayName + " &7Rewards");
        createInventory(title, size);

        rewardSlots = new ArrayList<>();
        for (int i = 0; i < rewardsThisPage && (start + i) < rewards.size(); i++) {
            TierReward reward = rewards.get(start + i);
            int slot = i;
            rewardSlots.add(slot);

            List<String> lore = new ArrayList<>(reward.getDisplayLore());
            ItemStack rewardItem = new ItemBuilder(reward.getDisplayMaterial(), 1, reward.getDisplayData())
                    .name(reward.getDisplayName())
                    .lore(lore)
                    .build();

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

        int closeSlot;
        if (needsNavRow) {
            closeSlot = 49;
        } else if (rows >= 4) {
            closeSlot = (rows * 9) - 5;
        } else {
            closeSlot = (rows * 9) - 1;
        }

        setItem(closeSlot, new ItemBuilder(Material.ARROW)
                .name(MessageUtil.colorize("&c&lClose"))
                .build());

        if (needsNavRow) {
            if (currentPage > 0) {
                setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&7\u00ab Previous Page"))
                        .build());
            }
            if (currentPage < totalPages - 1) {
                setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtil.colorize("&aNext Page &7\u00bb"))
                        .build());
            }
        }

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
                new TierMenuGui(plugin, player).open();
                return;
            }
        } else {
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

    }
}
