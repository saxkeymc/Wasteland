package dev.r3faced.minecurse.wasteland.editor;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.managers.TierManager;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the in-game preview reward editor.
 * <p>
 * Flow:
 * <ol>
 *   <li>Admin runs /wasteland addpreviewreward &lt;tier&gt;</li>
 *   <li>A 54-slot editable inventory opens. The admin drags items into it.</li>
 *   <li>On close, every non-empty item in the inventory is captured.</li>
 *   <li>The admin is prompted in chat, one reward at a time, to enter the
 *       chance (0-100) and the command to execute.</li>
 *   <li>After all rewards are processed, they are saved to tiers.yml and
 *       the tier cache is reloaded.</li>
 * </ol>
 * <p>
 * The preview item is display-only. The player never receives the item
 * itself — only the configured command executes silently when the reward
 * is won.
 */
public class PreviewRewardEditor implements Listener {

    private final WastelandPlugin plugin;

    /** Tracks players who currently have an editor inventory open. */
    private final Map<UUID, Integer> editingTiers = new ConcurrentHashMap<>();

    /** Tracks players who are in the chat-prompt phase. */
    private final Map<UUID, PromptState> promptStates = new ConcurrentHashMap<>();

    public PreviewRewardEditor(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the editor inventory for the given admin and tier.
     */
    public void openEditor(Player player, int tier) {
        Inventory inv = Bukkit.createInventory(null, 54,
                MessageUtil.colorize("&8Preview Reward Editor &7\u00bb &aTier " + tier));

        // Info item in the bottom-middle.
        ItemStack info = new ItemBuilder(Material.SIGN)
                .name("&e&lInstructions")
                .lore(
                        "&7Place items in this inventory to set them",
                        "&7as preview rewards for Tier " + tier + ".",
                        "",
                        "&7When you close this inventory, you will",
                        "&7be prompted in chat for the command and",
                        "&7chance for each reward.",
                        "",
                        "&cClose the inventory when done."
                )
                .build();
        inv.setItem(49, info);

        player.openInventory(inv);
        editingTiers.put(player.getUniqueId(), tier);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!editingTiers.containsKey(uuid)) return;

        int tier = editingTiers.remove(uuid);

        // Capture all non-empty items (skip the info item at slot 49).
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < event.getInventory().getSize(); i++) {
            if (i == 49) continue;
            ItemStack item = event.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(MessageUtil.colorize("&cNo items were placed. Preview rewards not updated."));
            return;
        }

        // Start the chat-prompt phase.
        PromptState state = new PromptState(tier, items);
        promptStates.put(uuid, state);

        player.sendMessage(MessageUtil.colorize("&a&l=== Preview Reward Editor ==="));
        player.sendMessage(MessageUtil.colorize("&7You placed &f" + items.size() + " &7item(s)."));
        player.sendMessage(MessageUtil.colorize("&7You will now be prompted for the &fchance &7and &fcommand &7for each."));
        player.sendMessage(MessageUtil.colorize("&7Type &fcancel &7at any time to abort."));
        player.sendMessage(MessageUtil.colorize(""));
        promptNext(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!promptStates.containsKey(uuid)) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            promptStates.remove(uuid);
            player.sendMessage(MessageUtil.colorize("&cPreview reward editing cancelled. No changes were saved."));
            return;
        }

        PromptState state = promptStates.get(uuid);

        if (state.stage == 0) {
            // Parse chance
            try {
                double chance = Double.parseDouble(message);
                chance = Math.max(0, Math.min(100, chance));
                state.chances.add(chance);
                state.stage = 1;

                ItemStack currentItem = state.items.get(state.index);
                String itemName = getItemName(currentItem);
                player.sendMessage(MessageUtil.colorize(
                        "&aReward " + (state.index + 1) + "/" + state.items.size() + " &7\u00bb &f" + itemName));
                player.sendMessage(MessageUtil.colorize("&7Type the command to execute when this reward is won:"));
                player.sendMessage(MessageUtil.colorize("&7Use &f%player% &7for the player name. Example: &fgive %player% diamond 64"));
            } catch (NumberFormatException e) {
                player.sendMessage(MessageUtil.colorize("&cInvalid number. Please enter a chance between 0 and 100:"));
            }
        } else if (state.stage == 1) {
            state.commands.add(message);
            state.index++;
            state.stage = 0;

            if (state.index >= state.items.size()) {
                final PromptState finalState = state;
                promptStates.remove(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> saveRewards(player, finalState));
            } else {
                promptNext(player);
            }
        }
    }

    private void promptNext(Player player) {
        PromptState state = promptStates.get(player.getUniqueId());
        if (state == null || state.index >= state.items.size()) return;

        ItemStack item = state.items.get(state.index);
        String itemName = getItemName(item);

        player.sendMessage(MessageUtil.colorize(
                "&aReward " + (state.index + 1) + "/" + state.items.size() + " &7\u00bb &f" + itemName));
        player.sendMessage(MessageUtil.colorize("&7Enter the chance (0-100) for this reward:"));
    }

    private void saveRewards(Player player, PromptState state) {
        List<TierManager.PreviewRewardEntry> entries = new ArrayList<>();

        for (int i = 0; i < state.items.size(); i++) {
            ItemStack item = state.items.get(i);
            ItemMeta meta = item.getItemMeta();

            Material material = item.getType();
            short data = item.getDurability();
            String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : "&f" + material.name();
            List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : new ArrayList<String>();

            Map<String, Integer> enchants = new HashMap<>();
            if (meta != null && meta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> ench : meta.getEnchants().entrySet()) {
                    enchants.put(ench.getKey().getName(), ench.getValue());
                }
            }

            List<String> itemFlags = new ArrayList<>();
            if (meta != null && meta.getItemFlags() != null) {
                for (org.bukkit.inventory.ItemFlag flag : meta.getItemFlags()) {
                    itemFlags.add(flag.name());
                }
            }

            double chance = i < state.chances.size() ? state.chances.get(i) : 100.0;
            List<String> commands = new ArrayList<>();
            if (i < state.commands.size()) {
                commands.add(state.commands.get(i));
            }

            entries.add(new TierManager.PreviewRewardEntry(
                    material, data, name, lore, enchants, itemFlags, chance, commands
            ));
        }

        plugin.getTierManager().savePreviewRewards(state.tier, entries);

        player.sendMessage(MessageUtil.colorize(
                "&a&lSaved! &7" + entries.size() + " reward(s) saved to Tier " + state.tier + "."));
        player.sendMessage(MessageUtil.colorize(
                "&7The rewards are now live in the /wasteland tiers GUI."));
    }

    private String getItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name();
    }

    private static class PromptState {
        final int tier;
        final List<ItemStack> items;
        final List<Double> chances = new ArrayList<>();
        final List<String> commands = new ArrayList<>();
        int index = 0;
        int stage = 0; // 0 = chance, 1 = command

        PromptState(int tier, List<ItemStack> items) {
            this.tier = tier;
            this.items = items;
        }
    }
}
