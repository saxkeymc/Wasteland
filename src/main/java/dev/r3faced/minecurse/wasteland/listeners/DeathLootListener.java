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

/**
 * When a player kills another player in a Wasteland world, the victim's
 * inventory items are stored in the killer's Wasteland Backpack instead
 * of dropping on the ground.
 * <p>
 * The backpack is accessible via /wasteland backpack or the last slot
 * of the main menu GUI.
 */
public class DeathLootListener implements Listener {

    private final WastelandPlugin plugin;

    public DeathLootListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Only in wasteland worlds.
        if (!plugin.getWastelandWorldManager().isWastelandWorld(victim.getWorld())) return;

        // Check if the victim was killed by a player.
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) return;

        // Only if the killer is also in a wasteland world.
        if (!plugin.getWastelandWorldManager().isWastelandWorld(killer.getWorld())) return;

        // Collect the victim's inventory items.
        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack item : victim.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                // Don't store omni tools or wasteland armor.
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

        // Clear the victim's drops so items don't spawn on the ground.
        event.getDrops().clear();

        // Store the loot in the killer's backpack.
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
