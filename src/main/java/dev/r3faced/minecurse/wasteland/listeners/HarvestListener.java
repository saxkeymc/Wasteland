package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class HarvestListener implements Listener {

    private final WastelandPlugin plugin;

    public HarvestListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTierLockedBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FARMING) return;

        plugin.getTierLockManager().handleBlockBreak(event, SkillType.FARMING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FARMING) return;

        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FARMING)) return;

        Block block = event.getBlock();
        String blockType = block.getType().name();
        int xp = plugin.getSkillManager().getXpForBlock(SkillType.FARMING, blockType);
        if (xp <= 0) return;

        event.setCancelled(true);

        plugin.getSkillManager().awardXp(player, SkillType.FARMING, xp, WastelandXpCause.BLOCK_BREAK, blockType);

        plugin.getDustManager().awardDust(player,
                plugin.getDustManager().getDefaultDustPerAction(SkillType.FARMING));

        tryRollMoneyDrop(player);

        final org.bukkit.Location blockLoc = block.getLocation();
        final Player p = player;
        final Material origMat = block.getType();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.sendBlockChange(blockLoc, Material.BEDROCK, (byte) 0);
            plugin.getFakeBlockManager().addFakeBlock(p, blockLoc, origMat);
        }, 1L);

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                p.sendBlockChange(blockLoc, origMat, (byte) 0);
            }
            plugin.getFakeBlockManager().removeFakeBlock(p, blockLoc);
        }, 121L);
    }

    private void tryRollMoneyDrop(Player player) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        if (!cfg.getBoolean("mining-money-drops.enabled", true)) return;
        double chance = cfg.getDouble("mining-money-drops.chance", 5.0);
        if (ThreadLocalRandom.current().nextDouble() * 100.0 >= chance) return;
        int min = cfg.getInt("mining-money-drops.min-amount", 5000);
        int max = cfg.getInt("mining-money-drops.max-amount", 7000);
        int amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        String command = cfg.getString("mining-money-drops.command", "eco give %player% {amount}")
                .replace("%player%", player.getName()).replace("{amount}", String.valueOf(amount));
        try {
            org.bukkit.Bukkit.getServer().dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
        } catch (Exception e) { return; }
        String prefix = dev.r3faced.minecurse.wasteland.utils.MessageUtil
                .colorize(plugin.getConfigManager().getMessages().getString("prefix", ""));
        player.sendMessage(dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                cfg.getString("mining-money-drops.message", "{prefix}&aYou found &2${amount} &awhile farming!")
                        .replace("{prefix}", prefix).replace("{amount}", String.valueOf(amount))));
    }
}
