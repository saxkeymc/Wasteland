package dev.r3faced.minecurse.wasteland.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Sends action bar messages to players on Spigot 1.8.8 via NMS reflection.
 * Uses PacketPlayOutChat with action bar type (byte 2).
 */
public final class ActionBarUtil {

    private ActionBarUtil() {}

    private static String VERSION;
    private static boolean initialized = false;
    private static boolean available = false;

    static {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = pkg.substring(pkg.lastIndexOf('.') + 1);
            // Test that the required classes exist.
            Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent");
            Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutChat");
            initialized = true;
            available = true;
        } catch (Exception e) {
            initialized = true;
            available = false;
        }
    }

    /**
     * Send an action bar message to a player.
     *
     * @param player  the player
     * @param message the message (already colorized)
     */
    public static void sendActionBar(Player player, String message) {
        if (!initialized || !available || player == null || message == null) return;

        try {
            String nmsPackage = "net.minecraft.server." + VERSION + ".";

            // Create the chat component from JSON.
            Class<?> chatSerializerClass = Class.forName(nmsPackage + "IChatBaseComponent$ChatSerializer");
            Class<?> chatComponentClass = Class.forName(nmsPackage + "IChatBaseComponent");

            // Build JSON: {"text":"message"}
            String json = "{\"text\":\"" + escapeJson(message) + "\"}";
            Object chatComponent = chatSerializerClass.getMethod("a", String.class)
                    .invoke(null, json);

            // Create the packet with action bar type (byte 2).
            Class<?> packetClass = Class.forName(nmsPackage + "PacketPlayOutChat");
            Object packet = packetClass.getConstructor(chatComponentClass, byte.class)
                    .newInstance(chatComponent, (byte) 2);

            // Send the packet to the player.
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            playerConnection.getClass().getMethod("sendPacket", Class.forName(nmsPackage + "Packet"))
                    .invoke(playerConnection, packet);

        } catch (Exception e) {
            // Silently fail — action bar is not critical.
        }
    }

    /** Escape special JSON characters in a string. */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
