package dev.r3faced.minecurse.wasteland.commands;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles /mining, /chopping, /farming, /fishing commands.
 * Replaces the player's current omni tool with the requested skill's tool.
 * Only works in a Wasteland world.
 */
public class SkillSwitchCommand implements CommandExecutor {

    private final WastelandPlugin plugin;

    public SkillSwitchCommand(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Must be in a Wasteland world.
        if (!plugin.getWastelandWorldManager().isWastelandWorld(player.getWorld())) {
            player.sendMessage("§cYou must be in a Wasteland world to use this command!");
            return true;
        }

        // Determine which skill based on the command name.
        SkillType skill;
        String cmdName = command.getName().toLowerCase();
        switch (cmdName) {
            case "mining":
                skill = SkillType.MINING;
                break;
            case "chopping":
                skill = SkillType.WOODCUTTING;
                break;
            case "farming":
                skill = SkillType.FARMING;
                break;
            case "fishing":
                skill = SkillType.FISHING;
                break;
            default:
                return false;
        }

        // Remove ALL omni tools from the player's inventory.
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            for (SkillType s : SkillType.values()) {
                if (plugin.getToolManager().isOmniTool(item, s)) {
                    player.getInventory().setItem(i, null);
                    break;
                }
            }
        }

        // Give the new tool.
        plugin.getToolManager().giveOmniTool(player, skill);
        player.updateInventory();

        return true;
    }
}
