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

public class EnchantCancelListener implements Listener {

    private final WastelandPlugin plugin;

    public EnchantCancelListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();
            if (plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) {
                int victimTier = plugin.getDataManager().getPlayerData(victim.getUniqueId()).getTier();
                int attackerTier = plugin.getDataManager().getPlayerData(attacker.getUniqueId()).getTier();
                if (victimTier >= 5 && attackerTier >= 5) return;
                event.setCancelled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) {
                int victimTier = plugin.getDataManager().getPlayerData(victim.getUniqueId()).getTier();
                if (victimTier >= 5) return;
                event.setCancelled(true);
                return;
            }
        }
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (plugin.getWastelandWorldManager().isWastelandWorld(attacker.getWorld())) {
                int attackerTier = plugin.getDataManager().getPlayerData(attacker.getUniqueId()).getTier();
                if (attackerTier >= 5) return;
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();
            if (plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) {
                String entityType = event.getEntity().getType().name();
                if (!entityType.equals("FISHING_HOOK")) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
