package dev.r3faced.minecurse.wasteland.utils;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;

/**
 * Centralised message utility.
 * Handles color codes, hex colors (§x format for 1.16+, silent no-op on 1.8),
 * prefix substitution, and optional PlaceholderAPI expansion.
 */
public final class MessageUtil {

    private static boolean papiPresent;

    static {
        papiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    private MessageUtil() {}

    /**
     * Translate & colour codes and attempt hex (#RRGGBB) conversion.
     */
    public static String colorize(String input) {
        if (input == null) return "";
        // Hex color support for 1.16+ (silently no-op on 1.8 since those codes don't exist)
        input = translateHex(input);
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Translate &#RRGGBB → §x§R§R§G§G§B§B for 1.16+.
     * On 1.8 the §x format will simply render as the closest legacy colour by the client.
     */
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

    /**
     * Read a message from messages.yml, apply prefix, colour, and optional PAPI for a player.
     * <p>
     * The {prefix} placeholder is replaced AFTER colorizing the rest of the
     * message to avoid double-processing of color codes.
     */
    public static String getMessage(WastelandPlugin plugin, String path, Player player) {
        String raw = plugin.getConfigManager().getMessages().getString(path, "&cMissing message: " + path);
        String prefix = plugin.getConfigManager().getMessages().getString("prefix", "&2&lWasteland &8\u2022 &f");
        // Colorize the entire message first (translates & codes in the message body).
        // Then replace {prefix} with the colorized prefix.
        // This avoids double-processing: the prefix's & codes are translated
        // independently, then inserted into the already-colorized message.
        raw = colorize(raw);
        raw = raw.replace("{prefix}", colorize(prefix));
        if (papiPresent && player != null) {
            raw = PlaceholderAPI.setPlaceholders(player, raw);
        }
        return raw;
    }

    /**
     * Read a message from messages.yml without a player context (no PAPI substitution).
     */
    public static String getMessage(WastelandPlugin plugin, String path) {
        return getMessage(plugin, path, null);
    }

    /**
     * Colorise a list of strings.
     */
    public static List<String> colorizeList(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(colorize(s));
        }
        return result;
    }
}
