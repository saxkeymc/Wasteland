package dev.r3faced.minecurse.wasteland.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TierReward {

    private final double chance;

    private final Material displayMaterial;

    private final short displayData;

    private final String displayName;

    private final List<String> displayLore;

    private final Map<String, Integer> displayEnchants;

    private final List<String> displayItemFlags;

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

    public boolean rollSuccess() {
        if (chance >= 100.0) return true;
        if (chance <= 0.0)   return false;
        return Math.random() * 100.0 < chance;
    }
}
