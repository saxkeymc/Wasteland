package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.WastelandChangeReason;
import dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Detects players entering Wasteland worlds (on join or world change) and
 * ensures they hold the correct Omni Tool. Also loads player data on join,
 * saves/unloads on quit, and flushes any unsaved Wasteland playtime
 * accumulated since the last periodic tick.
 * <p>
 * Playtime accounting:
 *   • Join:        if joining directly into a WL world, stamp
 *                  {@code inWastelandSince = now}.
 *   • WorldChange: if leaving a WL world, flush elapsed playtime and
 *                  clear the timestamp. If entering a WL world, stamp
 *                  the timestamp.
 *   • Quit:        if currently in a WL world, flush elapsed playtime
 *                  and clear the timestamp.
 *   • Periodic task (PlaytimeTask) handles the ongoing accumulation.
 */
public class WorldChangeListener implements Listener {

    private final WastelandPlugin plugin;

    public WorldChangeListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load data into cache
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);

        // ── Grant Tier 1 rewards on first join ──────────────────────────────
        // Tier 1 is unlocked by default (required-level: 0 in tiers.yml), but
        // the rewards are only added when checkTierUnlock fires — which only
        // happens on level-up. To make sure new players actually get their
        // Tier 1 rewards, we check here: if the player hasn't claimed Tier 1
        // rewards yet AND meets the Tier 1 requirements (which everyone does
        // by default), grant the Tier 1 rewards now.
        // We use a special claim key "tier_1_startup" to track this so it
        // only happens once per player.
        if (!data.getClaimedTiers().contains("tier_1_startup")) {
            // Use the tier manager to roll and add Tier 1 rewards.
            // We mark it as a startup grant by adding the key first.
            data.getClaimedTiers().add("tier_1_startup");
            // Roll Tier 1 rewards and add to backpack.
            try {
                java.util.List<dev.r3faced.minecurse.wasteland.model.TierReward> rewards =
                        plugin.getTierManager().getRewards(1);
                int added = 0;
                for (dev.r3faced.minecurse.wasteland.model.TierReward reward : rewards) {
                    if (reward.rollSuccess()) {
                        boolean stored = plugin.getApi().addStoredReward(player, new dev.r3faced.minecurse.wasteland.model.StoredReward(
                                reward.getDisplayMaterial(),
                                reward.getDisplayData(),
                                reward.getDisplayName(),
                                reward.getDisplayLore(),
                                reward.getCommands()
                        ), WastelandChangeReason.REWARD);
                        if (stored) {
                            added++;
                        }
                    }
                }
                if (added > 0) {
                    plugin.getDataManager().savePlayer(uuid);
                    // Send a delayed message so it appears after the player fully joins.
                    final Player p = player;
                    final int count = added;
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (p.isOnline()) {
                            String msg = dev.r3faced.minecurse.wasteland.utils.MessageUtil
                                    .getMessage(plugin, "rewards.added-to-storage")
                                    .replace("{count}", String.valueOf(count))
                                    .replace("{tier}", "1");
                            p.sendMessage(msg);
                        }
                    }, 40L); // 2 seconds delay
                }
            } catch (Exception ignored) {
                // Fail silently — don't break join if rewards fail to roll.
            }
        }

        // If joining directly into a Wasteland world, start the playtime clock.
        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        if (wwm.isWastelandWorld(player.getWorld())) {
            data.setInWastelandSince(System.currentTimeMillis());
        } else {
            data.setInWastelandSince(0L);
        }

        // Give tool if already in a wasteland world
        checkAndGiveTool(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        boolean wasInWl = wwm.isWastelandWorld(event.getFrom());
        boolean nowInWl = wwm.isWastelandWorld(player.getWorld());
        long now = System.currentTimeMillis();

        if (wasInWl && !nowInWl) {
            // Leaving a Wasteland world — flush elapsed playtime since the
            // last tick and stop the clock.
            flushSession(data, now);
            plugin.getDataManager().savePlayer(player.getUniqueId());
        } else if (!wasInWl && nowInWl) {
            // Entering a Wasteland world — start the clock.
            data.setInWastelandSince(now);
        }
        // If both were WL worlds, the timestamp stays as-is (continuous session).

        // Re-equip the appropriate Omni Tool for the new world.
        checkAndGiveTool(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        if (data != null) {
            // Flush any unflushed playtime before saving.
            flushSession(data, System.currentTimeMillis());
        }

        // Save player data, then unload from cache.
        plugin.getDataManager().savePlayer(uuid);
        plugin.getDataManager().unloadPlayer(uuid);
    }

    /**
     * If the player's inWastelandSince is non-zero, compute the elapsed
     * time since that timestamp and add it to the player's playtime,
     * then reset the timestamp to 0.
     */
    private void flushSession(PlayerData data, long now) {
        long since = data.getInWastelandSince();
        if (since > 0L) {
            long elapsed = (now - since) / 1000L;
            if (elapsed > 0) {
                data.addPlaytimeSeconds(elapsed);
            }
            data.setInWastelandSince(0L);
        }
    }

    private void checkAndGiveTool(Player player) {
        String worldName = player.getWorld().getName();
        SkillType skill = plugin.getToolManager().getSkillForWorld(worldName);
        if (skill != null) {
            plugin.getToolManager().giveOmniTool(player, skill);
        }
    }
}
