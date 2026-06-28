package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Cancels ALL custom enchant procs from EnchantmentAPI / CurseEnchants
 * in Wasteland worlds.
 * <p>
 * Since we don't have the EnchantmentAPI JAR at compile time, we
 * intercept the Bukkit events that custom enchants use to proc:
 * <ul>
 *   <li>EntityDamageByEntityEvent — combat enchants (Abyss, Sharpness, etc.)</li>
 *   <li>EntityDamageEvent — defensive enchants (Mirror, etc.)</li>
 *   <li>ProjectileLaunchEvent — projectile enchants</li>
 * </ul>
 * <p>
 * In Wasteland worlds, ALL damage is already cancelled by
 * WastelandWorldProtectionListener (no PvP, no fall, no fire, etc.).
 * Since custom enchants proc ON damage events, and those events are
 * cancelled, the enchants effectively can't fire.
 * <p>
 * This listener runs at LOWEST priority to ensure that if any enchant
 * plugin tries to proc at LOWEST, it sees the event as already
 * cancelled (by another plugin) and skips its logic.
 * <p>
 * Additionally, non-damage enchant effects (potions, particles, etc.)
 * are neutralized because WastelandWorldProtectionListener cancels
 * ALL damage, meaning enchant procs that deal damage have no effect.
 */
public class EnchantCancelListener implements Listener {

    private final WastelandPlugin plugin;

    public EnchantCancelListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancel enchant procs at the LOWEST priority. Most enchant plugins
     * listen at NORMAL or HIGHEST. By running at LOWEST, we ensure that
     * if the event was already cancelled (by WastelandWorldProtectionListener
     * at HIGHEST, or by Bukkit itself), enchant plugins see it as cancelled.
     * <p>
     * However, since HIGHEST > LOWEST in execution order, this LOWEST
     * handler runs FIRST (before HIGHEST). So we can't rely on the HIGHEST
     * cancellation here. Instead, we cancel the event ourselves at LOWEST
     * if the player is in a wasteland world.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        // Cancel at LOWEST priority so enchant plugins (which usually
        // listen at NORMAL or HIGHEST) see the event as cancelled
        // and skip their proc logic.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check both the victim AND the attacker.
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) {
                // Allow enchants to proc in the PvP zone.
                if (plugin.getPvpZoneManager().isInPvpZone(victim.getLocation())) return;
                event.setCancelled(true);
                return;
            }
        }
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (plugin.getWastelandWorldManager().isWastelandWorld(attacker.getWorld())) {
                // Allow enchants to proc in the PvP zone.
                if (plugin.getPvpZoneManager().isInPvpZone(attacker.getLocation())) return;
                event.setCancelled(true);
            }
        }
    }

    /**
     * Cancel projectile-based enchant procs (but NOT fishing hooks).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            if (plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) {
                String entityType = event.getEntity().getType().name();
                // Don't cancel fishing hooks — needed for fishing minigame.
                if (!entityType.equals("FISHING_HOOK")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
