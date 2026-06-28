package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Makes all Wasteland worlds safe zones:
 * <ul>
 *   <li>No PvP — players can't damage each other.</li>
 *   <li>No damage at all — fall, fire, drowning, suffocation, etc.</li>
 *   <li>No hunger/saturation loss.</li>
 * </ul>
 * <p>
 * All events are cancelled SILENTLY — no message is sent to the player.
 * This applies to ALL Wasteland worlds (mining, woodcutting, farming,
 * fishing) as configured in config.yml under wasteland-worlds.
 */
public class WastelandWorldProtectionListener implements Listener {

    private final WastelandPlugin plugin;

    public WastelandWorldProtectionListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancel ALL damage in wasteland worlds — fall, fire, drowning,
     * suffocation, contact (cactus), etc. Runs at HIGHEST priority so
     * it overrides other plugins. No message sent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        // Allow damage if the player is Tier 5 (PvP enabled everywhere for Tier 5).
        int playerTier = plugin.getDataManager().getPlayerData(player.getUniqueId()).getTier();
        if (playerTier >= 5) return;
        // Cancel silently — no message.
        event.setCancelled(true);
        player.setFireTicks(0);
    }

    /**
     * Cancel PvP (player-vs-player damage) in wasteland worlds.
     * Runs at HIGHEST priority. No message sent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) return;
        // Allow PvP if BOTH players are Tier 5.
        int victimTier = plugin.getDataManager().getPlayerData(victim.getUniqueId()).getTier();
        int attackerTier = plugin.getDataManager().getPlayerData(attacker.getUniqueId()).getTier();
        if (victimTier >= 5 && attackerTier >= 5) return;
        // Cancel silently.
        event.setCancelled(true);
    }

    /**
     * Cancel hunger/saturation loss in wasteland worlds.
     * No message sent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        // Cancel silently — player keeps full hunger.
        event.setCancelled(true);
    }
}
