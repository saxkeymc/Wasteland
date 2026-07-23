package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

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

    public void setPos1(Location loc) {
        this.pos1 = loc;
        saveZone();
    }

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

    public boolean isZoneSet() {
        return zoneSet;
    }

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

    public Location getPos1() { return pos1; }

    public Location getPos2() { return pos2; }
}
