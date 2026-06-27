package dev.r3faced.minecurse.wasteland.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single reward that has been unlocked and stored in the
 * player's virtual reward backpack, waiting to be claimed.
 * <p>
 * Each stored reward carries:
 * <ul>
 *   <li>{@code displayMaterial / displayData / displayName / displayLore} — what is shown in the GUI.</li>
 *   <li>{@code commands} — hidden console commands executed when the reward is claimed.</li>
 * </ul>
 * <p>
 * The chance roll happens at unlock time (in TierManager.checkTierUnlock).
 * Only successful rolls produce a StoredReward; the chance field is not
 * persisted because it's no longer needed after the roll.
 * <p>
 * Rewards with identical display items (same material, data, and name)
 * stack visually in the GUI. The actual commands still execute
 * individually when claimed.
 */
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

    /**
     * Returns true if this reward has the same display identity as another
     * (same material, data, and name). Used for visual stacking in the GUI.
     */
    public boolean displayMatches(StoredReward other) {
        if (other == null) return false;
        return this.displayMaterial == other.displayMaterial
            && this.displayData == other.displayData
            && (this.displayName == null ? other.displayName == null : this.displayName.equals(other.displayName));
    }
}
