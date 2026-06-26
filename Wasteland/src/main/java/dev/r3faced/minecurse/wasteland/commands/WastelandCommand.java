package dev.r3faced.minecurse.wasteland.commands;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.menus.CollectMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.HelpMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.MainMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.StatsMenuGui;
import dev.r3faced.minecurse.wasteland.gui.menus.TierMenuGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles all /wasteland (and /wl) sub-commands.
 *
 * <pre>
 * /wasteland                                  — open main GUI            [wasteland.use]
 * /wasteland collect                          — open collect GUI         [wasteland.use]
 * /wasteland claim                            — alias for collect        [wasteland.use]
 * /wasteland tiers                            — open tier browser        [wasteland.use]
 * /wasteland stats                            — open statistics GUI      [wasteland.use]
 * /wasteland help                             — open help GUI            [wasteland.use]
 * /wasteland playtime                         — view your playtime       [wasteland.use]
 * /wasteland reload                           — reload plugin            [wasteland.admin]
 * /wasteland give <player> <skill>            — give omni tool           [wasteland.admin]
 * /wasteland setlevel <p> <s> <l>             — set level                [wasteland.admin]
 * /wasteland addxp <p> <s> <amt>              — add xp                   [wasteland.admin]
 * /wasteland removexp <p> <s> <amt>           — remove xp                [wasteland.admin]
 * /wasteland settier <p> <t>                  — set tier                 [wasteland.admin]
 * /wasteland reset <player>                   — reset player             [wasteland.admin]
 * /wasteland setteleportmining                — set mining teleport      [wasteland.admin]
 * /wasteland setteleportchopping              — set chopping teleport    [wasteland.admin]
 * /wasteland setteleportfarming               — set farming teleport     [wasteland.admin]
 * /wasteland setteleportfishing               — set fishing teleport     [wasteland.admin]
 * </pre>
 */
public class WastelandCommand implements CommandExecutor, TabCompleter {

    private final WastelandPlugin plugin;

    private static final List<String> SKILLS = Arrays.asList("mining", "woodcutting", "farming", "fishing");
    private static final List<String> PLAYER_SUBCOMMANDS = Arrays.asList(
            "collect", "claim", "tiers", "stats", "help", "playtime"
    );
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "reload", "give", "setlevel", "addxp", "removexp", "settier", "reset",
            "setteleportmining", "setteleportchopping", "setteleportfarming", "setteleportfishing"
    );

    public WastelandCommand(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /wasteland (no args) — main menu
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(MessageUtil.getMessage(plugin, "player-only"));
                return true;
            }
            if (!sender.hasPermission("wasteland.use")) {
                sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission"));
                return true;
            }
            new MainMenuGui(plugin, (Player) sender).open();
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // ── Player commands ────────────────────────────────────────────────

            case "collect":
            case "claim": {
                if (!(sender instanceof Player)) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-only")); return true; }
                if (!sender.hasPermission("wasteland.use")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                new CollectMenuGui(plugin, (Player) sender).open();
                return true;
            }

            case "tiers": {
                if (!(sender instanceof Player)) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-only")); return true; }
                if (!sender.hasPermission("wasteland.use")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                new TierMenuGui(plugin, (Player) sender).open();
                return true;
            }

            case "stats": {
                if (!(sender instanceof Player)) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-only")); return true; }
                if (!sender.hasPermission("wasteland.use")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                new StatsMenuGui(plugin, (Player) sender).open();
                return true;
            }

            case "help": {
                if (!(sender instanceof Player)) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-only")); return true; }
                if (!sender.hasPermission("wasteland.use")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                new HelpMenuGui(plugin, (Player) sender).open();
                return true;
            }

            case "playtime": {
                if (!(sender instanceof Player)) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-only")); return true; }
                if (!sender.hasPermission("wasteland.use")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                Player p = (Player) sender;
                PlayerData data = plugin.getDataManager().getPlayerData(p.getUniqueId());
                String formatted = PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());
                String msg = MessageUtil.getMessage(plugin, "playtime.view")
                        .replace("{playtime}", formatted);
                p.sendMessage(msg);
                return true;
            }

            // ── Teleport setup commands ────────────────────────────────────────

            case "setteleportmining":
                return handleSetTeleport(sender, SkillType.MINING);
            case "setteleportchopping":
            case "setteleportwoodcutting":
                return handleSetTeleport(sender, SkillType.WOODCUTTING);
            case "setteleportfarming":
                return handleSetTeleport(sender, SkillType.FARMING);
            case "setteleportfishing":
                return handleSetTeleport(sender, SkillType.FISHING);

            // ── Admin commands ─────────────────────────────────────────────────

            case "reload": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                plugin.reload();
                sender.sendMessage(MessageUtil.getMessage(plugin, "plugin-reloaded"));
                return true;
            }

            case "give": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(usage("/wasteland give <player> <skill>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1])); return true; }
                SkillType skill = SkillType.fromString(args[2]);
                if (skill == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-skill")); return true; }
                plugin.getToolManager().giveOmniTool(target, skill);
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.gave-tool")
                        .replace("{player}", target.getName())
                        .replace("{skill}", skill.getKey()));
                return true;
            }

            case "setlevel": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                if (args.length < 4) { sender.sendMessage(usage("/wasteland setlevel <player> <skill> <level>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1])); return true; }
                SkillType skill = SkillType.fromString(args[2]);
                if (skill == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-skill")); return true; }
                int level;
                try { level = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-number").replace("{input}", args[3])); return true;
                }
                int cap = plugin.getSkillManager().getLevelCap(skill);
                level = Math.max(0, Math.min(level, cap));
                PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                data.setLevel(skill, level);
                data.setXp(skill, plugin.getSkillManager().xpRequiredForLevel(skill, level));
                plugin.getTierManager().checkTierUnlock(target);
                plugin.getDataManager().savePlayer(target.getUniqueId());
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.set-level")
                        .replace("{player}", target.getName())
                        .replace("{skill}", skill.getKey())
                        .replace("{level}", String.valueOf(level)));
                return true;
            }

            case "addxp": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                if (args.length < 4) { sender.sendMessage(usage("/wasteland addxp <player> <skill> <amount>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1])); return true; }
                SkillType skill = SkillType.fromString(args[2]);
                if (skill == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-skill")); return true; }
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-number").replace("{input}", args[3])); return true;
                }
                plugin.getSkillManager().awardXp(target, skill, amount);
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.added-xp")
                        .replace("{player}", target.getName())
                        .replace("{skill}", skill.getKey())
                        .replace("{amount}", String.valueOf(amount)));
                return true;
            }

            case "removexp": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                if (args.length < 4) { sender.sendMessage(usage("/wasteland removexp <player> <skill> <amount>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1])); return true; }
                SkillType skill = SkillType.fromString(args[2]);
                if (skill == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-skill")); return true; }
                long amount;
                try { amount = Long.parseLong(args[3]); } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-number").replace("{input}", args[3])); return true;
                }
                PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                long newXp = Math.max(0L, data.getXp(skill) - amount);
                data.setXp(skill, newXp);
                // Recalculate level from new XP
                int newLevel = 0;
                int cap = plugin.getSkillManager().getLevelCap(skill);
                while (newLevel < cap && plugin.getSkillManager().xpRequiredForLevel(skill, newLevel + 1) <= newXp) {
                    newLevel++;
                }
                data.setLevel(skill, newLevel);
                plugin.getDataManager().savePlayer(target.getUniqueId());
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.removed-xp")
                        .replace("{player}", target.getName())
                        .replace("{skill}", skill.getKey())
                        .replace("{amount}", String.valueOf(amount)));
                return true;
            }

            case "settier": {
                if (!sender.hasPermission("wasteland.admin")) { sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission")); return true; }
                if (args.length < 3) { sender.sendMessage(usage("/wasteland settier <player> <tier>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1])); return true; }
                int tier;
                try { tier = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-number").replace("{input}", args[2])); return true;
                }
                if (tier < 1 || tier > dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "invalid-tier")
                            .replace("{max}", String.valueOf(dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT)));
                    return true;
                }
                PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
                data.setTier(tier);
                plugin.getDataManager().savePlayer(target.getUniqueId());
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.set-tier")
                        .replace("{player}", target.getName())
                        .replace("{tier}", String.valueOf(tier)));
                return true;
            }

            case "reset": {
                if (!sender.hasPermission("wasteland.admin") && !sender.hasPermission("wasteland.*")) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(usage("/wasteland reset <player>"));
                    sender.sendMessage(usage("/wasteland reset all confirm"));
                    return true;
                }

                // ── /wasteland reset all confirm ──────────────────────────────
                if (args[1].equalsIgnoreCase("all")) {
                    if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                        // Send confirmation prompt — require "confirm" to proceed.
                        sender.sendMessage(MessageUtil.getMessage(plugin, "admin.reset-all-confirm"));
                        return true;
                    }
                    // Reset every online player + every cached player.
                    int resetCount = 0;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.getDataManager().resetPlayer(online.getUniqueId());
                        resetCount++;
                    }
                    // Also reset any cached-but-offline players (the data
                    // manager's resetPlayer already handles this via the cache).
                    sender.sendMessage(MessageUtil.getMessage(plugin, "admin.reset-all-complete")
                            .replace("{count}", String.valueOf(resetCount)));
                    return true;
                }

                // ── /wasteland reset <player> ────────────────────────────────
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "player-not-found").replace("{player}", args[1]));
                    return true;
                }
                plugin.getDataManager().resetPlayer(target.getUniqueId());
                sender.sendMessage(MessageUtil.getMessage(plugin, "admin.reset-player")
                        .replace("{player}", target.getName()));
                return true;
            }

            default: {
                if (sender instanceof Player) {
                    new MainMenuGui(plugin, (Player) sender).open();
                } else {
                    sender.sendMessage(MessageUtil.getMessage(plugin, "player-only"));
                }
                return true;
            }
        }
    }

    /** Handle /wasteland setteleport<skill> — saves the player's current location. */
    private boolean handleSetTeleport(CommandSender sender, SkillType skill) {
        if (!sender.hasPermission("wasteland.admin")) {
            sender.sendMessage(MessageUtil.getMessage(plugin, "no-permission"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.getMessage(plugin, "player-only"));
            return true;
        }
        Player player = (Player) sender;
        plugin.getTeleportManager().setTeleport(skill, player.getLocation());
        String msg = MessageUtil.getMessage(plugin, "admin.set-teleport")
                .replace("{skill}", skill.getKey());
        player.sendMessage(msg);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String s : PLAYER_SUBCOMMANDS) {
                if (s.startsWith(partial)) completions.add(s);
            }
            if (sender.hasPermission("wasteland.admin")) {
                for (String s : ADMIN_SUBCOMMANDS) {
                    if (s.startsWith(partial)) completions.add(s);
                }
            }
            return completions;
        }

        String sub = args[0].toLowerCase();

        // Player name completions
        if ((sub.equals("give") || sub.equals("setlevel") || sub.equals("addxp")
                || sub.equals("removexp") || sub.equals("settier") || sub.equals("reset"))
                && args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) completions.add(p.getName());
            }
            return completions;
        }

        // Skill completions
        if ((sub.equals("give") || sub.equals("setlevel") || sub.equals("addxp")
                || sub.equals("removexp"))
                && args.length == 3) {
            String partial = args[2].toLowerCase();
            for (String s : SKILLS) {
                if (s.startsWith(partial)) completions.add(s);
            }
            return completions;
        }

        // Tier completions (settier now uses 3 args: <player> <tier>)
        if (sub.equals("settier") && args.length == 3) {
            for (int i = 1; i <= dev.r3faced.minecurse.wasteland.managers.TierManager.TIER_COUNT; i++) {
                completions.add(String.valueOf(i));
            }
            return completions;
        }

        return completions;
    }

    private String usage(String usage) {
        return MessageUtil.colorize("&cUsage: " + usage);
    }
}
