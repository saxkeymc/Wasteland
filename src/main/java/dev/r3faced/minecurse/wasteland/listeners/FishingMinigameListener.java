package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandXpCause;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingMinigameListener implements Listener {

    private final WastelandPlugin plugin;

    private final Map<UUID, BukkitRunnable> pendingBites = new HashMap<>();

    private final Map<UUID, CatchSession> activeSessions = new HashMap<>();

    private final Map<UUID, org.bukkit.entity.FishHook> activeHooks = new HashMap<>();

    private final Map<UUID, Long> catchCooldown = new HashMap<>();

    private static final long CATCH_DURATION_TICKS = 200L;
    private static final long TICK_INTERVAL = 4L;

    public FishingMinigameListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        String worldName = player.getWorld().getName();
        SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
        if (worldSkill != SkillType.FISHING) return;

        ItemStack hand = player.getItemInHand();
        if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) return;

        PlayerFishEvent.State state = event.getState();

        if (state == PlayerFishEvent.State.FISHING) {
            Long cooldownUntil = catchCooldown.get(player.getUniqueId());
            if (cooldownUntil != null && System.currentTimeMillis() < cooldownUntil) {
                event.setCancelled(true);
                return;
            }
            catchCooldown.remove(player.getUniqueId());

            if (event.getHook() != null) {
                activeHooks.put(player.getUniqueId(), event.getHook());
            }

            final Player p = player;
            final UUID puuid = player.getUniqueId();

            if (pendingBites.containsKey(puuid)) {
                pendingBites.get(puuid).cancel();
            }
            activeSessions.remove(puuid);

            int delayTicks = 100 + ThreadLocalRandom.current().nextInt(101);
            BukkitRunnable biteTask = new BukkitRunnable() {
                @Override
                public void run() {
                    pendingBites.remove(puuid);
                    if (!p.isOnline()) return;
                    if (!plugin.getWastelandWorldManager().isWastelandWorld(p.getWorld())) return;

                    ItemStack h = p.getItemInHand();
                    if (!plugin.getToolManager().isOmniTool(h, SkillType.FISHING)) return;
                    if (activeSessions.containsKey(puuid)) return;

                    org.bukkit.entity.FishHook hook = activeHooks.get(puuid);
                    if (hook == null || !hook.isValid()) return;

                    String caughtType = getRandomCatchType();
                    Material fishMat = getFishMaterial(caughtType);
                    Location fishLoc = hook.getLocation().clone().add(0, 0.5, 0);
                    Item fishItem = p.getWorld().dropItem(fishLoc, new ItemStack(fishMat));
                    fishItem.setVelocity(new org.bukkit.util.Vector(0, 0.1, 0));
                    fishItem.setPickupDelay(Integer.MAX_VALUE);
                    try {
                        fishItem.setCustomName(ChatColor.AQUA + "Fish!");
                        fishItem.setCustomNameVisible(true);
                    } catch (Exception ignored) {}

                    try {
                        p.playSound(p.getLocation(), Sound.SPLASH, 0.7f, 1.2f);
                    } catch (Exception ignored) {}

                    CatchSession session = new CatchSession(p, caughtType, fishItem);
                    activeSessions.put(puuid, session);
                    startCatchProgress(session);
                }
            };
            biteTask.runTaskLater(plugin, delayTicks);
            pendingBites.put(puuid, biteTask);

        } else if (state == PlayerFishEvent.State.CAUGHT_FISH) {
            event.setCancelled(true);

        } else if (state == PlayerFishEvent.State.IN_GROUND || state == PlayerFishEvent.State.FAILED_ATTEMPT) {
            UUID uuid = player.getUniqueId();
            if (pendingBites.containsKey(uuid)) {
                pendingBites.get(uuid).cancel();
                pendingBites.remove(uuid);
            }
            CatchSession session = activeSessions.remove(uuid);
            if (session != null && session.fishItem != null) {
                session.fishItem.remove();
            }
            activeHooks.remove(uuid);
        }
    }

    private void startCatchProgress(CatchSession session) {
        final Player player = session.player;
        final UUID uuid = player.getUniqueId();
        final String caughtType = session.caughtType;
        final Item fishItem = session.fishItem;

        new BukkitRunnable() {
            @Override
            public void run() {
                CatchSession s = activeSessions.get(uuid);
                if (s == null) {
                    this.cancel();
                    return;
                }

                if (!player.isOnline()) {
                    activeSessions.remove(uuid);
                    if (fishItem != null) fishItem.remove();
                    this.cancel();
                    return;
                }

                String worldName = player.getWorld().getName();
                SkillType worldSkill = plugin.getToolManager().getSkillForWorld(worldName);
                if (worldSkill != SkillType.FISHING) {
                    activeSessions.remove(uuid);
                    if (fishItem != null) fishItem.remove();
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                ItemStack hand = player.getItemInHand();
                if (!plugin.getToolManager().isOmniTool(hand, SkillType.FISHING)) {
                    activeSessions.remove(uuid);
                    if (fishItem != null) fishItem.remove();
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                org.bukkit.entity.FishHook hook = activeHooks.get(uuid);
                if (hook == null || !hook.isValid()) {
                    activeSessions.remove(uuid);
                    if (fishItem != null) fishItem.remove();
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.RED + "Fishing cancelled!");
                    this.cancel();
                    return;
                }

                if (fishItem != null && fishItem.isValid()) {
                    Location fishLoc = fishItem.getLocation();
                    Location hookLoc = hook.getLocation();
                    org.bukkit.util.Vector dir = hookLoc.toVector().subtract(fishLoc.toVector()).normalize().multiply(0.05);
                    fishItem.setVelocity(dir);
                }

                long elapsed = System.currentTimeMillis() - s.startTime;
                int progress = (int) ((elapsed * 100L) / (CATCH_DURATION_TICKS * 50L));

                if (progress >= 100) {
                    progress = 100;
                    activeSessions.remove(uuid);

                    if (fishItem != null) fishItem.remove();

                    if (hook != null) hook.remove();
                    activeHooks.remove(uuid);

                    catchCooldown.put(uuid, System.currentTimeMillis() + 2000L);

                    String bar = buildProgressBar(100);
                    dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                        ChatColor.GREEN + bar + " " + ChatColor.GREEN + "100% " + ChatColor.GOLD + "Caught!");

                    player.sendTitle(
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Caught!",
                        ChatColor.GRAY + "Reeled in a fish!"
                    );

                    try {
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.5f, 1.5f);
                    } catch (Exception ignored) {}

                    int xp = plugin.getSkillManager().getXpForBlock(SkillType.FISHING, caughtType);
                    if (xp > 0) {
                        plugin.getSkillManager().awardXp(player, SkillType.FISHING, xp,
                                WastelandXpCause.FISHING, caughtType);
                    }

                    plugin.getDustManager().awardDust(player,
                            plugin.getDustManager().getDefaultDustPerAction(SkillType.FISHING));

                    tryRollMoneyDrop(player);

                    this.cancel();
                    return;
                }

                String bar = buildProgressBar(progress);
                dev.r3faced.minecurse.wasteland.utils.ActionBarUtil.sendActionBar(player,
                    ChatColor.AQUA + bar + " " + ChatColor.WHITE + progress + "%");
            }
        }.runTaskTimer(plugin, 0L, TICK_INTERVAL);
    }

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
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) { return; }
        String prefix = dev.r3faced.minecurse.wasteland.utils.MessageUtil
                .colorize(plugin.getConfigManager().getMessages().getString("prefix", ""));
        player.sendMessage(dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                cfg.getString("mining-money-drops.message", "{prefix}&aYou found &2${amount} &awhile fishing!")
                        .replace("{prefix}", prefix).replace("{amount}", String.valueOf(amount))));
    }

    private String getRandomCatchType() {
        String[] types = {"RAW_FISH", "RAW_SALMON", "CLOWNFISH", "PUFFERFISH",
                "ENCHANTED_BOOK", "BOW", "FISHING_ROD", "NAME_TAG", "SADDLE", "DEFAULT"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    private Material getFishMaterial(String caughtType) {
        try {
            return Material.valueOf(caughtType);
        } catch (Exception e) {
            return Material.RAW_FISH;
        }
    }

    private static class CatchSession {
        final Player player;
        final String caughtType;
        final Item fishItem;
        final long startTime;

        CatchSession(Player player, String caughtType, Item fishItem) {
            this.player = player;
            this.caughtType = caughtType;
            this.fishItem = fishItem;
            this.startTime = System.currentTimeMillis();
        }
    }
}
