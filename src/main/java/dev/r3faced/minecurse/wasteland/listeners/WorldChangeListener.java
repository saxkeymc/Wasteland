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

public class WorldChangeListener implements Listener {

    private final WastelandPlugin plugin;

    public WorldChangeListener(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerData data = plugin.getDataManager().getPlayerData(uuid);

        if (!data.getClaimedTiers().contains("tier_1_startup")) {
            data.getClaimedTiers().add("tier_1_startup");
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
                    }, 40L);
                }
            } catch (Exception ignored) {
            }
        }

        WastelandWorldManager wwm = plugin.getWastelandWorldManager();
        if (wwm.isWastelandWorld(player.getWorld())) {
            data.setInWastelandSince(System.currentTimeMillis());
        } else {
            data.setInWastelandSince(0L);
        }

        checkAndGiveTool(player);

        if (plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) {
            plugin.getArmorManager().giveArmorSet(player);
        }

        applyPlayerVisibility(player, data.isSettingSeePlayers());
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
            flushSession(data, now);
            plugin.getDataManager().savePlayer(player.getUniqueId());

            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.updateInventory();

            player.setLevel(data.getSavedVanillaLevel());
            player.setExp(data.getSavedVanillaXp());
            player.setTotalExperience(data.getSavedVanillaLevel());
        } else if (!wasInWl && nowInWl) {
            data.setInWastelandSince(now);

            data.setSavedVanillaLevel(player.getLevel());
            data.setSavedVanillaXp(player.getExp());

            plugin.getArmorManager().giveArmorSet(player);
        }

        checkAndGiveTool(player);

        applyPlayerVisibility(player, data.isSettingSeePlayers());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        if (data != null) {
            flushSession(data, System.currentTimeMillis());
        }

        plugin.getDataManager().savePlayer(uuid);
        plugin.getDataManager().unloadPlayer(uuid);
    }

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
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                data.setActiveSkill(skill);
            }
        }
    }

    private void applyPlayerVisibility(Player player, boolean seePlayers) {
        for (Player other : player.getWorld().getPlayers()) {
            if (other == player) continue;
            if (seePlayers) {
                player.showPlayer(other);
            } else {
                player.hidePlayer(other);
            }
        }
    }

    private void removeAllOmniTools(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            for (SkillType skill : SkillType.values()) {
                if (plugin.getToolManager().isOmniTool(item, skill)) {
                    inv.setItem(i, null);
                    break;
                }
            }
        }
        player.updateInventory();
    }
}
