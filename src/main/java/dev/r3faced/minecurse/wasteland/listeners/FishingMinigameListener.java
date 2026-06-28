package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fishing minigame — two phases:
 * <p>
 * <strong>Phase 1: Bite Window (5-10 seconds after cast)</strong>
 * <ul>
 *   <li>After casting, wait 5-10 seconds for a bite.</li>
 *   <li>When the bite happens: SPLASH sound, title "Hooked!", action bar "CLICK TO HOOK!".</li>
 *   <li>The player has 3 seconds to right-click (reel in).</li>
 *   <li>If they hook it in time → Phase 2.</li>
 *   <li>If they DON'T hook it in time → "The fish got away!" message, minigame ends.</li>
 * </ul>
 * <p>
 * <strong>Phase 2: Reel-in Progress (10 seconds)</strong>
 * <ul>
 *   <li>Action bar shows progress bar: [■■■□□□□□□□□□□□□□□□□□] 35%</li>
 *   <li>The player must HOLD the rod for 10 seconds.</li>
 *   <li>At 100% → catch! XP + dust + possible money drop.</li>
 *   <li>If the player switches items or leaves → "Fishing cancelled!".</li>
 * </ul>
 */
public class FishingMinigameListener implements Listener {

    private final WastelandPlugin plugin;

    /** Tracks pending bite tasks: UUID → BukkitRunnable. */
    private final Map<UUID, BukkitRunnable> pendingBites = new HashMap<>();

    /** Tracks bite-window state: UUID → BiteWindow (waiting for player to hook). */
    private final Map<UUID, BiteWindow> biteWindows = new HashMap<>();

    /** Tracks active reel-in sessions: UUID → ReelSession. */
    private final Map<UUID, ReelSession> activeSessions = new HashMap<>();

    /** Tracks catch cooldown: UUID → epoch-millis when the player can cast again. */
    private final Map<UUID, Long> catchCooldown = new HashMap<>();

    /** Tracks the fishing hook entity per player: UUID → FishHook. */
    private final Map<UUID, org.bukkit.entity.FishHook> activeHooks = new HashMap<>();

    /** 10 seconds in ticks for the reel-in phase. */
    private static final long REEL_DURATION_TICKS = 200L;
    private static final long TICK_INTERVAL = 4L; // Update every 4 ticks (0.2s)

    /** 3 seconds to hook the fish after the bite. */
    private static final long HOOK_WINDOW_TICKS = 60L;

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

        if (state == PlayerFishEvent.State.FISHING) {
            // Check catch cooldown — prevent immediate re-cast after a catch.
            Long cooldownUntil = catchCooldown.get(player.getUniqueId());
            if (cooldownUntil != null && System.currentTimeMillis() < cooldownUntil) {
                // Still on cooldown — cancel this cast.
                event.setCancelled(true);
                return;
            }
            catchCooldown.remove(player.getUniqueId());

            // Player just cast the rod. Store the hook entity so we can
            // check if it's still in the water later.
            if (event.getHook() != null) {
                activeHooks.put(player.getUniqueId(), event.getHook());
            }

            // Player just cast the rod. Schedule a guaranteed bite in 5-10 seconds.
            final Player p = player;
            final UUID puuid = player.getUniqueId();

            // Cancel any existing pending bite.
            if (pendingBites.containsKey(puuid)) {
                pendingBites.get(puuid).cancel();
            }
            // Cancel any existing bite window.
            if (biteWindows.containsKey(puuid)) {
                biteWindows.remove(puuid);
            }
            // Cancel any existing reel session — the player re-cast.
            if (activeSessions.containsKey(puuid)) {
                activeSessions.remove(puuid);
            }

            // Schedule a bite in 5-10 seconds (100-200 ticks).
            int delayTicks = 100 + ThreadLocalRandom.current().nextInt(101); // 5-10 seconds
            BukkitRunnable biteTask = new BukkitRunnable() {
                @Override
                public void run() {
                    pendingBites.remove(puuid);
                    if (!p.isOnline()) return;
                    if (!plugin.getWastelandWorldManager().isWastelandWorld(p.getWorld())) return;

                    // Check if the player is still holding the fishing rod.
                    ItemStack h = p.getItemInHand();
                    if (!plugin.getToolManager().isOmniTool(h, SkillType.FISHING)) return;

                    // Check if not already in a reel session.
                    if (activeSessions.containsKey(puuid)) return;

                    // ── BITE! ──
                    // Play splash sound.
                    try {
                        p.playSound(p.getLocation(), Sound.SPLASH, 0.7f, 1.2f);
                    } catch (Exception ignored) {}

                    // Show title "Hooked!" + subtitle.
                    p.sendTitle(
                        ChatColor.AQUA + "" + ChatColor.BOLD + "Hooked!",
                        ChatColor.GRAY + "Right-click to hook the fish!"
                    );

                    // Action bar.
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(p,
                        ChatColor.AQUA + "" + ChatColor.BOLD + "CLICK TO HOOK!");

                    // Open the bite window — player has 3 seconds to right-click.
                    BiteWindow window = new BiteWindow(p, getRandomCatchType());
                    biteWindows.put(puuid, window);

                    // Schedule the timeout — if the player doesn't hook in 3 seconds.
                    final String caughtType = window.caughtType;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (biteWindows.containsKey(puuid)) {
                            biteWindows.remove(puuid);
                            // Fish escaped.
                            dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(p,
                                ChatColor.RED + "The fish got away!");
                            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 0.3f, 1.0f);
                        }
                    }, HOOK_WINDOW_TICKS); // 3 seconds
                }
            };
            biteTask.runTaskLater(plugin, delayTicks);
            pendingBites.put(puuid, biteTask);

        } else if (state == PlayerFishEvent.State.CAUGHT_FISH) {
            // Cancel vanilla catches — we handle everything ourselves.
            event.setCancelled(true);

        } else if (state == PlayerFishEvent.State.IN_GROUND || state == PlayerFishEvent.State.FAILED_ATTEMPT) {
            // Player reeled in the rod without a bite (or the hook landed on ground,
            // or the hook was removed after a catch).
            // Clean up: cancel pending bite, remove bite window, remove active session,
            // and remove the hook entity.
            UUID uuid = player.getUniqueId();
            if (pendingBites.containsKey(uuid)) {
                pendingBites.get(uuid).cancel();
                pendingBites.remove(uuid);
            }
            biteWindows.remove(uuid);
            activeSessions.remove(uuid);
            activeHooks.remove(uuid);
            // Don't schedule a new bite — this is a cleanup, not a cast.
        }
    }

    /**
     * Listen for right-clicks during the bite window to hook the fish.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if there's an active bite window.
        if (!biteWindows.containsKey(uuid)) return;

        // Check if holding the fishing rod.
        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) return;

        // ── HOOKED! Start the reel-in phase. ──
        event.setCancelled(true);
        BiteWindow window = biteWindows.remove(uuid);

        // Play hook sound.
        try {
            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.5f, 1.5f);
        } catch (Exception ignored) {}

        // Start reel-in session.
        ReelSession session = new ReelSession(player, window.caughtType);
        activeSessions.put(uuid, session);
        startReelProgress(session);
    }

    /**
     * Start the 10-second reel-in progress bar.
     */
    private void startReelProgress(ReelSession session) {
        final Player player = session.player;
        final UUID uuid = player.getUniqueId();
        final String caughtType = session.caughtType;

        new BukkitRunnable() {
            @Override
            public void run() {
                ReelSession s = activeSessions.get(uuid);
                if (s == null) {
                    this.cancel();
                    return;
                }

                // Check if player is still online and in fishing world.
                if (!player.isOnline()) {
                    activeSessions.remove(uuid);
                    this.cancel();
                    return;
                }

                String worldName = player.getWorld().getName();
                SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
                if (worldSkill != SkillType.FISHING) {
                    activeSessions.remove(uuid);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                // Check if player is still holding the fishing rod.
                ItemStack hand = player.getItemInHand();
                if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) {
                    // Player switched items — abort.
                    activeSessions.remove(uuid);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                // Check if the fishing hook is still in the water.
                // If the player reeled in (hook removed), cancel the minigame.
                org.bukkit.entity.FishHook hook = activeHooks.get(uuid);
                if (hook == null || hook.isDead() || !hook.isValid()) {
                    // Hook was removed — player reeled in or it despawned.
                    activeSessions.remove(uuid);
                    activeHooks.remove(uuid);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                // Increment progress.
                long elapsed = System.currentTimeMillis() - s.startTime;
                int progress = (int) ((elapsed * 100L) / (REEL_DURATION_TICKS * 50L)); // 50ms per tick
                if (progress >= 100) {
                    progress = 100;

                    // ── CAUGHT! ──
                    activeSessions.remove(uuid);

                    // Remove the fishing hook from the water.
                    if (hook != null) {
                        hook.remove();
                    }
                    activeHooks.remove(uuid);

                    // Set a 2-second cooldown before the player can cast again.
                    // This prevents the hook-removal event from re-triggering
                    // a new fishing session.
                    catchCooldown.put(uuid, System.currentTimeMillis() + 2000L);

                    // Build the progress bar.
                    String bar = buildProgressBar(100);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.GREEN + bar + " " + ChatColor.GREEN + "100% " + ChatColor.GOLD + "Caught!");

                    // Show title.
                    player.sendTitle(
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Caught!",
                        ChatColor.GRAY + "Reeled in a fish!"
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

                    // Award Dust.
                    plugin.getDustManager().awardDust(player,
                            plugin.getDustManager().getDefaultDustPerAction(SkillType.FISHING));

                    // Random money drop.
                    tryRollMoneyDrop(player);

                    this.cancel();
                    return;
                }

                // Update action bar with progress bar.
                String bar = buildProgressBar(progress);
                dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                    ChatColor.AQUA + bar + " " + ChatColor.WHITE + progress + "%");
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

    /** Build a 20-char progress bar. */
    private String buildProgressBar(int percent) {
        int filled = (int) Math.round(percent / 100.0 * 20.0);
        if (filled > 20) filled = 20;
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.DARK_GRAY).append("[");
        for (int i = 0; i < filled; i++) sb.append(ChatColor.AQUA).append("|");
        for (int i = filled; i < 20; i++) sb.append(ChatColor.DARK_GRAY).append(".");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    /** Random money drop. */
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

    /** Returns a random catch type. */
    private String getRandomCatchType() {
        String[] types = {"RAW_FISH", "RAW_SALMON", "CLOWNFISH", "PUFFERFISH",
                "ENCHANTED_BOOK", "BOW", "FISHING_ROD", "NAME_TAG", "SADDLE", "DEFAULT"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    // ── Inner classes ──────────────────────────────────────────────────────────

    /** Bite window — the player has 3 seconds to right-click after the bite. */
    private static class BiteWindow {
        final Player player;
        final String caughtType;

        BiteWindow(Player player, String caughtType) {
            this.player = player;
            this.caughtType = caughtType;
        }
    }

    /** Reel session — the 10-second progress bar phase. */
    private static class ReelSession {
        final Player player;
        final String caughtType;
        final long startTime;

        ReelSession(Player player, String caughtType) {
            this.player = player;
            this.caughtType = caughtType;
            this.startTime = System.currentTimeMillis();
        }
    }
}
