package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages PvP zones in Wasteland worlds.
 * <p>
 * A PvP zone is a cuboid area where:
 * <ul>
 *   <li>Players CAN damage each other (PvP enabled).</li>
 *   <li>Custom enchants CAN proc.</li>
 *   <li>There is NO height limit — PvP works at any Y level.</li>
 * </ul>
 * <p>
 * Outside the PvP zone:
 * <ul>
 *   <li>No PvP (cancelled by WastelandWorldProtectionListener).</li>
 *   <li>No custom enchant procs (cancelled by EnchantCancelListener).</li>
 * </ul>
 * <p>
 * If a player falls below Y=90 in a Wasteland world, they are teleported
 * back to where they were.
 */
public class PvpZoneManager {

    private final WastelandPlugin plugin;
    private Location pos1;
    private Location pos2;
    private boolean zoneSet = false;

    public PvpZoneManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.contains("pvp-zone.pos1") && cfg.contains("pvp-zone.pos2")) {
            pos1 = (Location) cfg.get("pvp-zone.pos1");
            pos2 = (Location) cfg.get("pvp-zone.pos2");
            zoneSet = pos1 != null && pos2 != null;
        }
    }

    /** Set position 1 of the PvP zone. */
    public void setPos1(Location loc) {
        this.pos1 = loc;
        saveZone();
    }

    /** Set position 2 of the PvP zone. */
    public void setPos2(Location loc) {
        this.pos2 = loc;
        saveZone();
    }

    private void saveZone() {
        if (pos1 != null && pos2 != null) {
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("pvp-zone.pos1", pos1);
            cfg.set("pvp-zone.pos2", pos2);
            plugin.saveConfig();
            zoneSet = true;
        }
    }

    /** Returns true if a PvP zone has been set. */
    public boolean isZoneSet() {
        return zoneSet;
    }

    /**
     * Check if a location is inside the PvP zone.
     * Ignores Y level — PvP works at any height within the zone.
     */
    public boolean isInPvpZone(Location loc) {
        if (!zoneSet || pos1 == null || pos2 == null) return false;
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    /** Returns pos1 for display. */
    public Location getPos1() { return pos1; }

    /** Returns pos2 for display. */
    public Location getPos2() { return pos2; }
}
