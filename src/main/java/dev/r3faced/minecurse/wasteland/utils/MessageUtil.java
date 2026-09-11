package dev.r3faced.minecurse.wasteland.utils;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

public final class MessageUtil {

    private static boolean papiPresent;

    static {
        papiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private MessageUtil() {}

    public static String colorize(String input) {
        if (input == null) return "";
        input = translateHex(input);
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private static String translateHex(String msg) {
        if (!msg.contains("&#")) return msg;
        StringBuilder sb = new StringBuilder();
        char[] chars = msg.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 7 < chars.length && chars[i + 1] == '#') {
                String hex = msg.substring(i + 2, i + 8);
                sb.append('\u00A7').append('x');
                for (char c : hex.toCharArray()) {
                    sb.append('\u00A7').append(c);
                }
                i += 7;
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    public static String getMessage(WastelandPlugin plugin, String path, Player player) {
        String raw = plugin.getConfigManager().getMessages().getString(path, "&cMissing message: " + path);
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "&2&lWasteland &8\u2022 &f");
        raw = colorize(raw);
        raw = raw.replace("{prefix}", colorize(prefix));
        if (papiPresent && player != null) {
            raw = PlaceholderAPI.setPlaceholders(player, raw);
        }
        return raw;
    }

    public static String getMessage(WastelandPlugin plugin, String path) {
        return getMessage(plugin, path, null);
    }

    public static List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(colorize(s));
        }
        return result;
    }
}
