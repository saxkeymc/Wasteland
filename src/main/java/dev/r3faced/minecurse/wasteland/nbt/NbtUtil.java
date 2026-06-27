package dev.r3faced.minecurse.wasteland.nbt;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Utility class for reading and writing NBT string tags on ItemStacks.
 * Uses CraftBukkit/NMS reflection to remain compatible with Minecraft 1.8.8.
 */
public final class NbtUtil {

    private NbtUtil() {}

    private static final String VERSION;

    static {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        VERSION = pkg.substring(pkg.lastIndexOf('.') + 1);
    }

    /**
     * Set a string NBT tag on an ItemStack.
     *
     * @param item  the source item
     * @param key   the NBT key
     * @param value the string value to store
     * @return a new ItemStack with the tag applied (or the original on failure)
     */
    public static ItemStack setTag(ItemStack item, String key, String value) {
        try {
            Class<?> craftItemClass = getCraftClass("inventory.CraftItemStack");
            Class<?> nmsItemClass   = getNmsClass("ItemStack");
            Class<?> nbtTagClass    = getNmsClass("NBTTagCompound");
            Class<?> nbtStringClass = getNmsClass("NBTTagString");

            Method asNMSCopy = craftItemClass.getMethod("asNMSCopy", ItemStack.class);
            Object nmsItem = asNMSCopy.invoke(null, item);

            Method getTag = nmsItemClass.getMethod("getTag");
            Object tag = getTag.invoke(nmsItem);
            if (tag == null) {
                tag = nbtTagClass.newInstance();
            }

            // NBTTagString constructor differs between versions
            Object nbtString;
            try {
                // 1.15+: NBTTagString.a(String)
                Method factory = nbtStringClass.getMethod("a", String.class);
                nbtString = factory.invoke(null, value);
            } catch (NoSuchMethodException e) {
                // 1.8–1.14: new NBTTagString(String)
                nbtString = nbtStringClass.getConstructor(String.class).newInstance(value);
            }

            Method set = nbtTagClass.getMethod("set", String.class, getNmsClass("NBTBase"));
            set.invoke(tag, key, nbtString);

            Method setTag = nmsItemClass.getMethod("setTag", nbtTagClass);
            setTag.invoke(nmsItem, tag);

            Method asCraftMirror = craftItemClass.getMethod("asCraftMirror", nmsItemClass);
            return (ItemStack) asCraftMirror.invoke(null, nmsItem);

        } catch (Exception e) {
            return item;
        }
    }

    /**
     * Read a string NBT tag from an ItemStack.
     *
     * @param item the item to inspect
     * @param key  the NBT key
     * @return the string value or null if absent
     */
    public static String getTag(ItemStack item, String key) {
        if (item == null) return null;
        try {
            Class<?> craftItemClass = getCraftClass("inventory.CraftItemStack");
            Class<?> nmsItemClass   = getNmsClass("ItemStack");
            Class<?> nbtTagClass    = getNmsClass("NBTTagCompound");

            Method asNMSCopy = craftItemClass.getMethod("asNMSCopy", ItemStack.class);
            Object nmsItem = asNMSCopy.invoke(null, item);

            Method getTag = nmsItemClass.getMethod("getTag");
            Object tag = getTag.invoke(nmsItem);
            if (tag == null) return null;

            Method hasKey = nbtTagClass.getMethod("hasKey", String.class);
            if (!(Boolean) hasKey.invoke(tag, key)) return null;

            Method getString = nbtTagClass.getMethod("getString", String.class);
            return (String) getString.invoke(tag, key);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if an ItemStack has a specific NBT string key+value pair.
     */
    public static boolean hasTag(ItemStack item, String key, String expectedValue) {
        String actual = getTag(item, key);
        return expectedValue.equals(actual);
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static Class<?> getNmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + VERSION + "." + name);
    }

    private static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + name);
    }
}
