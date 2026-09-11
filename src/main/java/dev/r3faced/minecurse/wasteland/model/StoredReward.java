package dev.r3faced.minecurse.wasteland.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StoredReward {

    private final Material displayMaterial;
    private final short displayData;
    private final String displayName;
    private final List<String> displayLore;
    private final List<String> commands;

    public StoredReward(Material displayMaterial,
                        short displayData,
                        String displayName,
                        List<String> displayLore,
                        List<String> commands) {
        this.displayMaterial = displayMaterial;
        this.displayData     = displayData;
        this.displayName     = displayName;
        this.displayLore     = displayLore == null ? new ArrayList<>() : new ArrayList<>(displayLore);
        this.commands        = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
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

    public boolean displayMatches(StoredReward other) {
        if (other == null) return false;
        return this.displayMaterial == other.displayMaterial
            && this.displayData == other.displayData
            && (this.displayName == null ? other.displayName == null : this.displayName.equals(other.displayName));
    }
}
