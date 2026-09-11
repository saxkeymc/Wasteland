package dev.r3faced.minecurse.wasteland.listeners;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.Set;

public class CommandWhitelistListener implements Listener {

    private final WastelandPlugin plugin;
    private final Set<String> allowedCommands = new HashSet<>();
    private String blockedMessage;

    public CommandWhitelistListener(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        allowedCommands.clear();
        org.bukkit.configuration.file.FileConfiguration cfg =
                plugin.getConfigManager().getCommandsConfig();
        if (cfg == null) return;

        blockedMessage = cfg.getString("blocked-message",
                "&cYou cannot run that command in this world!");

        for (String cmd : cfg.getStringList("allowed-commands")) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                String normalized = cmd.trim().toLowerCase();
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                allowedCommands.add(normalized);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();

        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        if (player.isOp() ||
            player.hasPermission("wasteland.admin") ||
            player.hasPermission("wasteland.bypass-commands")) {
            return;
        }

        String raw = event.getMessage();
        if (raw.startsWith("/")) raw = raw.substring(1);
        if (raw.contains(":")) {
            raw = raw.substring(raw.indexOf(":") + 1);
        }
        String baseCommand = raw.split(" ")[0].toLowerCase();

        String originalRaw = event.getMessage();
        if (originalRaw.startsWith("/")) originalRaw = originalRaw.substring(1);
        String originalBase = originalRaw.split(" ")[0].toLowerCase();

        if (!allowedCommands.contains(baseCommand) && !allowedCommands.contains(originalBase)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.colorize(blockedMessage));
        }
    }
}
