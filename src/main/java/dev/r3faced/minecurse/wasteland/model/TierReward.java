package dev.r3faced.minecurse.wasteland.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single reward entry for a tier.
 * <p>
 * Each reward has:
 * <ul>
 *   <li>{@code chance} — independent probability (0-100) of being awarded.</li>
 *   <li>{@code displayMaterial / displayData / displayName / displayLore} — what is shown in the GUI.</li>
 *   <li>{@code displayEnchants} — enchantments applied to the display item.</li>
 *   <li>{@code displayItemFlags} — item flags applied to the display item (e.g. HIDE_ENCHANTS).</li>
 *   <li>{@code commands} — list of console commands executed (with %player% substitution) when this reward fires.</li>
 * </ul>
 * Commands are NEVER displayed anywhere in the GUI.
 */
public class TierReward {

    /** Probability this reward is awarded when the tier is claimed (0-100). */
    private final double chance;

    /** Material of the showcase item shown in the GUI. */
    private final Material displayMaterial;

    /** Item data / durability value for the showcase item. */
    private final short displayData;

    /** Showcase item display name (already colourised). */
    private final String displayName;

    /** Showcase item lore lines (already colourised). */
    private final List<String> displayLore;

    /** Enchantments applied to the display item (name → level). */
    private final Map<String, Integer> displayEnchants;

    /** Item flags applied to the display item (flag name strings). */
    private final List<String> displayItemFlags;

    /** Hidden console commands executed when this reward fires. */
    private final List<String> commands;

    public TierReward(double chance,
                      Material displayMaterial,
                      short displayData,
                      String displayName,
                      List<String> displayLore,
                      Map<String, Integer> displayEnchants,
                      List<String> displayItemFlags,
                      List<String> commands) {
        this.chance          = chance;
        this.displayMaterial = displayMaterial;
        this.displayData     = displayData;
        this.displayName     = displayName;
        this.displayLore     = displayLore == null ? new ArrayList<>() : displayLore;
        this.displayEnchants = displayEnchants == null ? new HashMap<>() : displayEnchants;
        this.displayItemFlags = displayItemFlags == null ? new ArrayList<>() : displayItemFlags;
        this.commands        = commands == null ? new ArrayList<>() : commands;
    }

    public double getChance() { return chance; }
    public Material getDisplayMaterial() { return displayMaterial; }
    public short getDisplayData() { return displayData; }
    public String getDisplayName() { return displayName; }
    public List<String> getDisplayLore() { return displayLore; }
    public Map<String, Integer> getDisplayEnchants() { return displayEnchants; }
    public List<String> getDisplayItemFlags() { return displayItemFlags; }
    public List<String> getCommands() { return commands; }

    /**
     * Returns true if a random roll (0-100) succeeds for this reward's chance.
     */
    public boolean rollSuccess() {
        if (chance >= 100.0) return true;
        if (chance <= 0.0)   return false;
        return Math.random() * 100.0 < chance;
    }
}
