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

/**
 * Blocks commands that are not on the whitelist while the player is inside
 * a Wasteland world. The whitelist is loaded from commands.yml and applies
 * to ALL wasteland worlds equally.
 * <p>
 * Players with the {@code wasteland.admin} or {@code wasteland.bypass-commands}
 * permission are exempt from this restriction.
 */
public class CommandWhitelistListener implements Listener {

    private final WastelandPlugin plugin;
    private final Set<String> allowedCommands = new HashSet<>();
    private String blockedMessage;

    public CommandWhitelistListener(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** (Re)load the whitelist from commands.yml. */
    public void reload() {
        allowedCommands.clear();
        org.bukkit.configuration.file.FileConfiguration cfg =
                plugin.getConfigManager().getCommandsConfig();
        if (cfg == null) return;

        blockedMessage = cfg.getString("blocked-message",
                "&cYou cannot run that command in this world!");

        for (String cmd : cfg.getStringList("allowed-commands")) {
            if (cmd != null && !cmd.trim().isEmpty()) {
                // Normalize: lowercase, strip leading "/" if present.
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

        // Only applies in wasteland worlds.
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) return;

        // Admins are exempt (so they can manage the server).
        if (player.isOp() ||
            player.hasPermission("wasteland.admin") ||
            player.hasPermission("wasteland.bypass-commands")) {
            return;
        }

        // Extract the base command (first word, without leading /).
        String raw = event.getMessage();
        if (raw.startsWith("/")) raw = raw.substring(1);
        // Handle plugin-prefixed commands like "minecraft:tp" or "essentials:home"
        // by stripping everything before the colon.
        if (raw.contains(":")) {
            raw = raw.substring(raw.indexOf(":") + 1);
        }
        String baseCommand = raw.split(" ")[0].toLowerCase();

        // Also check with the original command (before colon stripping).
        String originalRaw = event.getMessage();
        if (originalRaw.startsWith("/")) originalRaw = originalRaw.substring(1);
        String originalBase = originalRaw.split(" ")[0].toLowerCase();

        // Check if the command is on the whitelist.
        if (!allowedCommands.contains(baseCommand) && !allowedCommands.contains(originalBase)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtil.colorize(blockedMessage));
        }
    }
}
