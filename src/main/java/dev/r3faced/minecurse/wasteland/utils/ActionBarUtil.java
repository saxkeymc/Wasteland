package dev.r3faced.minecurse.wasteland.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ActionBarUtil {

    private ActionBarUtil() {}

    private static String VERSION;
    private static boolean initialized = false;
    private static boolean available = false;

    static {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = pkg.substring(pkg.lastIndexOf('.') + 1);
            Class.forName("net.minecraft.server." + VERSION + ".IChatBaseComponent");
            Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutChat");
            initialized = true;
            available = true;
        } catch (Exception e) {
            initialized = true;
            available = false;
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (!initialized || !available || player == null || message == null) return;

        try {
            String nmsPackage = "net.minecraft.server." + VERSION + ".";

            Class<?> chatSerializerClass = Class.forName(nmsPackage + "IChatBaseComponent$ChatSerializer");
            Class<?> chatComponentClass = Class.forName(nmsPackage + "IChatBaseComponent");

            String json = "{\"text\":\"" + escapeJson(message) + "\"}";
            Object chatComponent = chatSerializerClass.getMethod("a", String.class)
                    .invoke(null, json);

            Class<?> packetClass = Class.forName(nmsPackage + "PacketPlayOutChat");
            Object packet = packetClass.getConstructor(chatComponentClass, byte.class)
                    .newInstance(chatComponent, (byte) 2);

            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            playerConnection.getClass().getMethod("sendPacket", Class.forName(nmsPackage + "Packet"))
                    .invoke(playerConnection, packet);

        } catch (Exception e) {
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
