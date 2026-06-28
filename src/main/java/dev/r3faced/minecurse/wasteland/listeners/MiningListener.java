package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Listens for block break events in the mining world.
 * <p>
 * Three responsibilities:
 * <ol>
 *   <li><strong>Tier-locked blocks</strong> — delegates to
 *       {@link dev.r3faced.minecurse.wasteland.managers.TierLockManager}.</li>
 *   <li><strong>XP awarding</strong> — when the player holds the Mining Omni
 *       Tool and breaks a non-locked block that yields XP, XP is awarded.</li>
 *   <li><strong>Random money drops</strong> — when mining, there's a random
 *       chance to receive money ($5,000–$7,000 by default) via console
 *       command. Configurable in config.yml under mining-money-drops.</li>
 * </ol>
 */
public class MiningListener implements Listener {

    private final WastelandPlugin plugin;

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Tier-lock check — runs at HIGHEST priority so it can cancel the break
     * before the MONITOR handler awards XP / money.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        plugin.getTierLockManager().handleBlockBreak(event, SkillType.MINING);
    }

    /**
     * XP awarding + random money drops — runs at MONITOR priority, only if
     * the break was not cancelled.
     * <p>
     * Also cancels block drops so the player only gets XP, not the block
     * items. Blocks in wasteland worlds are only for getting XP.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.MINING)) return;

        String blockType = event.getBlock().getType().name();
        int xp = plugin.getSkillManager().getXpForBlock(SkillType.MINING, blockType);
        if (xp > 0) {
            plugin.getSkillManager().awardXp(player, SkillType.MINING, xp, WastelandXpCause.BLOCK_BREAK, blockType);
        }

        // ── Cancel block drops — blocks are only for XP, not items ──────────
        // We set the block to AIR so it doesn't drop anything. The player
        // already got XP above. This applies to ALL blocks broken with the
        // omni tool in the mining world (not just tier-locked ones).
        event.getBlock().setType(Material.AIR);

        // ── Award Dust ───────────────────────────────────────────────────────
        int dustAmount = plugin.getDustManager().getDefaultDustPerAction(SkillType.MINING);
        plugin.getDustManager().awardDust(player, dustAmount);

        // ── Random money drop ───────────────────────────────────────────────
        tryRollMoneyDrop(player);
    }

    /**
     * Roll for a random money drop. If successful, executes the configured
     * console command (e.g. "eco give %player% 5000") with a random amount
     * between min-amount and max-amount.
     */
    private void tryRollMoneyDrop(Player player) {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (!cfg.getBoolean("mining-money-drops.enabled", true)) return;

        double chance = cfg.getDouble("mining-money-drops.chance", 5.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;

        int min = cfg.getInt("mining-money-drops.min-amount", 5000);
        int max = cfg.getInt("mining-money-drops.max-amount", 7000);
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);

        String command = cfg.getString("mining-money-drops.command", "eco give %player% {amount}");
        command = command.replace("%player%", player.getName()).replace("{amount}", String.valueOf(amount));

        try {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to dispatch money-drop command '" + command + "': " + e.getMessage());
            return;
        }

        // Send the configurable message.
        String msgTemplate = cfg.getString("mining-money-drops.message",
                "{prefix}&aYou found &2${amount} &awhile mining!");
        // Get the prefix from messages.yml
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "");
        prefix = MessageUtil.colorize(prefix);
        String msg = MessageUtil.colorize(msgTemplate
                .replace("{prefix}", prefix)
                .replace("{amount}", String.valueOf(amount)));
        player.sendMessage(msg);
    }
}
