package dev.r3faced.minecurse.wasteland.api;

/**
 * Static access point for plugins that depend on Wasteland.
 *
 * <pre>
 * WastelandApi api = WastelandProvider.get();
 * api.addXp(player, SkillType.MINING, 50, WastelandXpCause.CUSTOM, "my-mask");
 * </pre>
 */
public final class WastelandProvider {

    private static WastelandApi api;

    private WastelandProvider() {
    }

    public static WastelandApi get() {
        if (api == null) {
            throw new IllegalStateException("Wasteland API is not available.");
        }
        return api;
    }

    public static WastelandApi getOrNull() {
        return api;
    }

    public static boolean isAvailable() {
        return api != null;
    }

    public static void register(WastelandApi wastelandApi) {
        if (wastelandApi == null) {
            throw new IllegalArgumentException("wastelandApi cannot be null");
        }
        api = wastelandApi;
    }

    public static void unregister(WastelandApi wastelandApi) {
        if (api == wastelandApi) {
            api = null;
        }
    }
}
