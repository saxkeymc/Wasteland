package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fishing minigame: when a player hooks a fish, they must hold the rod
 * for 8 seconds. The action bar shows progress (1% → 100%). At 100%,
 * the catch is awarded with XP. If the player releases (reels in) before
 * 100%, the catch is lost.
 * <p>
 * The minigame only applies in the fishing world.
 */
public class FishingMinigameListener implements Listener {

    private final WastelandPlugin plugin;

    /** Tracks active fishing sessions: UUID → progress (0-100). */
    private final Map<UUID, FishingSession> activeSessions = new HashMap<>();

    /** 8 seconds in ticks. */
    private static final long DURATION_TICKS = 160L;
    private static final long TICK_INTERVAL = 4L; // Update every 4 ticks (0.2s)

    public FishingMinigameListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Only in fishing world.
        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FISHING) return;

        // Must be holding the fishing omni tool.
        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) return;

        PlayerFishEvent.State state = event.getState();

        if (state == PlayerFishEvent.State.CAUGHT_FISH) {
            // A fish is on the hook — cancel the default catch and start the minigame.
            event.setCancelled(true);

            // Don't start if already in a session.
            if (activeSessions.containsKey(player.getUniqueId())) return;

            // Determine what was caught (for XP calculation later).
            String caughtType = "DEFAULT";
            if (event.getCaught() instanceof Item) {
                ItemStack caught = ((Item) event.getCaught()).getItemStack();
                caughtType = caught.getType().name();
            }

            // Start the minigame.
            FishingSession session = new FishingSession(player, caughtType);
            activeSessions.put(player.getUniqueId(), session);
            startProgressBar(session);
        }
    }

    /**
     * Start the action-bar progress bar. Updates every TICK_INTERVAL ticks.
     * At 100%, awards XP and plays a sound.
     */
    private void startProgressBar(FishingSession session) {
        final Player player = session.player;
        final UUID uuid = player.getUniqueId();
        final String caughtType = session.caughtType;

        new BukkitRunnable() {
            @Override
            public void run() {
                FishingSession s = activeSessions.get(uuid);
                if (s == null) {
                    this.cancel();
                    return;
                }

                // Check if player is still online and still in fishing world.
                if (!player.isOnline()) {
                    activeSessions.remove(uuid);
                    this.cancel();
                    return;
                }

                String worldName = player.getWorld().getName();
                SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
                if (worldSkill != SkillType.FISHING) {
                    activeSessions.remove(uuid);
                    this.cancel();
                    return;
                }

                // Check if player is still holding the fishing rod.
                ItemStack hand = player.getItemInHand();
                if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) {
                    // Player switched items — abort minigame.
                    activeSessions.remove(uuid);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(
                        player, ChatColor.RED + "Fishing cancelled!"
                    );
                    this.cancel();
                    return;
                }

                // Increment progress.
                long elapsed = System.currentTimeMillis() - s.startTime;
                int progress = (int) ((elapsed * 100L) / (DURATION_TICKS * 50L)); // 50ms per tick
                if (progress >= 100) {
                    progress = 100;
                    // Complete!
                    activeSessions.remove(uuid);

                    // Build the progress bar.
                    String bar = buildProgressBar(100);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(
                        player, ChatColor.GREEN + bar + " " + ChatColor.GREEN + "100% " + ChatColor.GOLD + "Caught!"
                    );

                    // Play success sound.
                    try {
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.5f, 1.5f);
                    } catch (Exception ignored) {}

                    // Award XP.
                    int xp = plugin.getSkillManager().getXpForBlock(SkillType.FISHING, caughtType);
                    if (xp > 0) {
                        plugin.getSkillManager().awardXp(player, SkillType.FISHING, xp,
                                WastelandXpCause.FISHING, caughtType);
                    }

                    // Random money drop chance.
                    tryRollMoneyDrop(player);

                    this.cancel();
                    return;
                }

                // Update action bar.
                String bar = buildProgressBar(progress);
                ChatColor color = progress < 100 ? ChatColor.AQUA : ChatColor.GREEN;
                dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(
                    player, color + bar + " " + ChatColor.WHITE + progress + "%"
                );
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

    /** Build a 20-char progress bar. */
    private String buildProgressBar(int percent) {
        int filled = (int) Math.round(percent / 100.0 * 20.0);
        if (filled > 20) filled = 20;
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.DARK_GRAY).append("[");
        for (int i = 0; i < filled; i++) sb.append(ChatColor.AQUA).append("■");
        for (int i = filled; i < 20; i++) sb.append(ChatColor.DARK_GRAY).append("□");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    /** Random money drop — same logic as MiningListener. */
    private void tryRollMoneyDrop(Player player) {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
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
            plugin.getLogger().warning("Failed to dispatch money-drop command: " + e.getMessage());
            return;
        }

        String prefix = dev.r3faced.minecurse.wasteland.utils.MessageUtil
                .colorize(plugin.getConfigManager().getMessages().getString("prefix", ""));
        String msg = dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                cfg.getString("mining-money-drops.message",
                        "{prefix}&aYou found &2${amount} &awhile fishing!")
                        .replace("{prefix}", prefix)
                        .replace("{amount}", String.valueOf(amount)));
        player.sendMessage(msg);
    }

    /** Simple session holder. */
    private static class FishingSession {
        final Player player;
        final String caughtType;
        final long startTime;

        FishingSession(Player player, String caughtType) {
            this.player = player;
            this.caughtType = caughtType;
            this.startTime = System.currentTimeMillis();
        }
    }
}
