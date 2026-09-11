package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class WastelandWorldProtectionListener implements Listener {

    private final WastelandPlugin plugin;

    public WastelandWorldProtectionListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        int playerTier = plugin.getDataManager().getPlayerData(player.getUniqueId()).getTier();
        if (playerTier >= 5) return;
        event.setCancelled(true);
        player.setFireTicks(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) return;
        int victimTier = plugin.getDataManager().getPlayerData(victim.getUniqueId()).getTier();
        int attackerTier = plugin.getDataManager().getPlayerData(attacker.getUniqueId()).getTier();
        if (victimTier >= 5 && attackerTier >= 5) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;
        event.setCancelled(true);
    }
}
