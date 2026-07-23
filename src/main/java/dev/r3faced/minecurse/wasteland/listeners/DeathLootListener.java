package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathLootListener implements Listener {

    private final WastelandPlugin plugin;

    public DeathLootListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        if (!plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) return;

        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return;

        if (!plugin.getWastelandWorldManager().isWastelandWorld(killer.getWorld())) return;

        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack item : victim.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                if (plugin.getToolManager().getToolSkill(item) != null) continue;
                if (plugin.getArmorManager().isWastelandArmor(item)) continue;
                loot.add(item.clone());
            }
        }
        for (ItemStack item : victim.getInventory().getArmorContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                if (plugin.getArmorManager().isWastelandArmor(item)) continue;
                loot.add(item.clone());
            }
        }

        if (loot.isEmpty()) return;

        event.getDrops().clear();

        PlayerData killerData = plugin.getDataManager().getPlayerData(killer.getUniqueId());
        for (ItemStack item : loot) {
            killerData.getBackpackItems().add(item);
        }
        plugin.getDataManager().savePlayer(killer.getUniqueId());

        killer.sendMessage(dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                "&2&l+" + loot.size() + " &7items added to your Wasteland Backpack!"));
        killer.sendMessage(dev.r3faced.minecurse.wasteland.utils.MessageUtil.colorize(
                "&7Use &2/wasteland backpack &7to view your loot."));
    }
}
