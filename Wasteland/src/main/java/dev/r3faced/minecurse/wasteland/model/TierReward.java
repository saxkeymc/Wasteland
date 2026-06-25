package dev.r3faced.minecurse.wasteland.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single reward entry for a (skill, tier) pair.
 * <p>
 * Each reward has:
 * <ul>
 *   <li>{@code chance} — independent probability (0-100) of being awarded when the tier is claimed.</li>
 *   <li>{@code displayMaterial / displayData / displayName / displayLore} — what is shown in the GUI.
 *       Commands are NEVER displayed anywhere.</li>
 *   <li>{@code commands} — list of console commands executed (with %player% substitution) when this reward fires.</li>
 * </ul>
 */
public class TierReward {

    /** Probability this reward is awarded when the tier is claimed (0-100). */
    private final double chance;

    /** Material of the showcase item shown in the GUI. */
    private final Material displayMaterial;

    /** Item data / durability value for the showcase item (used for stained glass, etc.). */
    private final short displayData;

    /** Showcase item display name (already colourised). */
    private final String displayName;

    /** Showcase item lore lines (already colourised). */
    private final List<String> displayLore;

    /** Hidden console commands executed when this reward fires. */
    private final List<String> commands;

    public TierReward(double chance,
                      Material displayMaterial,
                      short displayData,
                      String displayName,
                      List<String> displayLore,
                      List<String> commands) {
        this.chance          = chance;
        this.displayMaterial = displayMaterial;
        this.displayData     = displayData;
        this.displayName     = displayName;
        this.displayLore     = displayLore == null ? new ArrayList<>() : displayLore;
        this.commands        = commands == null ? new ArrayList<>() : commands;
    }

    public double getChance() {
        return chance;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    public short getDisplayData() {
        return displayData;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDisplayLore() {
        return displayLore;
    }

    public List<String> getCommands() {
        return commands;
    }

    /**
     * Returns true if a random roll (0-100) succeeds for this reward's chance.
     */
    public boolean rollSuccess() {
        if (chance >= 100.0) return true;
        if (chance <= 0.0)   return false;
        return Math.random() * 100.0 < chance;
    }
}
