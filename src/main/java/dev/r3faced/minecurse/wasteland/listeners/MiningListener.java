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

public class MiningListener implements Listener {

    private final WastelandPlugin plugin;

    public MiningListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.MINING) return;

        plugin.getTierLockManager().handleBlockBreak(event, SkillType.MINING);
    }

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

        if (xp <= 0 || !isOre(event.getBlock().getType())) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getFakeBlockManager().isFakeBedrock(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        plugin.getSkillManager().awardXp(player, SkillType.MINING, xp, WastelandXpCause.BLOCK_BREAK, blockType);

        int dustAmount = plugin.getDustManager().getDefaultDustPerAction(SkillType.MINING);
        plugin.getDustManager().awardDust(player, dustAmount);

        tryRollMoneyDrop(player);

        final org.bukkit.Location blockLoc = event.getBlock().getLocation();
        final Player p = player;
        final Material origMat = event.getBlock().getType();
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

    private boolean isOre(Material mat) {
        switch (mat) {
            case COAL_ORE:
            case IRON_ORE:
            case GOLD_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case LAPIS_ORE:
            case REDSTONE_ORE:
            case QUARTZ_ORE:
            case GLOWSTONE:
                return true;
            default:
                return false;
        }
    }

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

        String msgTemplate = cfg.getString("mining-money-drops.message",
                "{prefix}&aYou found &2${amount} &awhile mining!");
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "");
        prefix = MessageUtil.colorize(prefix);
        String msg = MessageUtil.colorize(msgTemplate
                .replace("{prefix}", prefix)
                .replace("{amount}", String.valueOf(amount)));
        player.sendMessage(msg);
    }
}
