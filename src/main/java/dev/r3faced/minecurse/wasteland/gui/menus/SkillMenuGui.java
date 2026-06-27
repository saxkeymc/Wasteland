package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill detail menu showing level, XP, tier, and a progress bar.
 * Opened when clicking a skill in the main menu.
 * <p>
 * The "Close" button returns the player to the Main Wasteland GUI rather
 * than closing the inventory.
 */
public class SkillMenuGui extends WastelandGui {

    private final SkillType skill;

    public SkillMenuGui(WastelandPlugin plugin, Player player, SkillType skill) {
        super(plugin, player);
        this.skill = skill;
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        String displayName = cfg.getString("skills." + skill.getKey() + ".display-name", skill.getKey());
        String title = MessageUtil.colorize(
                cfg.getString("skill-menu.title", "&7Wasteland &8\u2022 &a{skill}")
                        .replace("{skill}", displayName));
        int size = cfg.getInt("skill-menu.size", 54);
        createInventory(title, size);

        // Border + filler
        ItemStack border = buildFiller(cfg, "skill-menu.border-item", Material.STAINED_GLASS_PANE, (short) 15);
        ItemStack filler = buildFiller(cfg, "skill-menu.fill-item",   Material.STAINED_GLASS_PANE, (short) 7);
        fill(filler);
        drawBorder(border);

        // Level display
        int levelSlot = cfg.getInt("skill-menu.slots.level-display.slot", 13);
        Material levelMat = parseMaterial(cfg.getString("skill-menu.slots.level-display.material", "EXP_BOTTLE"), Material.EXP_BOTTLE);
        String levelName = MessageUtil.colorize(cfg.getString("skill-menu.slots.level-display.name", "&a&lSkill Level"));
        int cap = plugin.getSkillManager().getLevelCap(skill);
        long nextXp = plugin.getSkillManager().xpRequiredForLevel(skill, data.getLevel(skill) + 1);
        List<String> levelLore = applyPlaceholders(
                cfg.getStringList("skill-menu.slots.level-display.lore"), data, cap, nextXp);
        setItem(levelSlot, new ItemBuilder(levelMat).name(levelName).lore(levelLore).build());

        // Progress bar
        int progressSlot = cfg.getInt("skill-menu.slots.progress-bar.slot", 22);
        Material progressMat = parseMaterial(cfg.getString("skill-menu.slots.progress-bar.material", "PAPER"), Material.PAPER);
        String progressName = MessageUtil.colorize(cfg.getString("skill-menu.slots.progress-bar.name", "&7Progress"));
        String bar = plugin.getSkillManager().getProgressBar(skill, data);
        List<String> progressLore = new ArrayList<>();
        progressLore.add(bar);
        progressLore.add(MessageUtil.colorize("&7" + data.getXp(skill) + " / " + nextXp + " XP"));
        setItem(progressSlot, new ItemBuilder(progressMat).name(progressName).lore(progressLore).build());

        // Back button (returns to main menu)
        int backSlot = cfg.getInt("skill-menu.slots.back.slot", 45);
        Material backMat = parseMaterial(cfg.getString("skill-menu.slots.back.material", "ARROW"), Material.ARROW);
        String backName = MessageUtil.colorize(cfg.getString("skill-menu.slots.back.name", "&7\u00ab Back"));
        setItem(backSlot, new ItemBuilder(backMat).name(backName).build());

        // View Tiers
        int tiersSlot = cfg.getInt("skill-menu.slots.tiers.slot", 49);
        Material tiersMat = parseMaterial(cfg.getString("skill-menu.slots.tiers.material", "NETHER_STAR"), Material.NETHER_STAR);
        String tiersName = MessageUtil.colorize(cfg.getString("skill-menu.slots.tiers.name", "&e&lView Tiers"));
        setItem(tiersSlot, new ItemBuilder(tiersMat).name(tiersName).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        // Both Back and Close buttons return to the Main Wasteland GUI.
        if (slot == cfg.getInt("skill-menu.slots.back.slot", 45)) {
            new MainMenuGui(plugin, player).open();
        } else if (slot == cfg.getInt("skill-menu.slots.tiers.slot", 49)) {
            new TierMenuGui(plugin, player).open();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> applyPlaceholders(List<String> raw, PlayerData data, int cap, long nextXp) {
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            line = line
                    .replace("{level}",     String.valueOf(data.getLevel(skill)))
                    .replace("{max_level}", String.valueOf(cap))
                    .replace("{xp}",        String.valueOf(data.getXp(skill)))
                    .replace("{next_xp}",   String.valueOf(nextXp))
                    .replace("{tier}",      String.valueOf(data.getTier()));
            result.add(MessageUtil.colorize(line));
        }
        return result;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try { return Material.valueOf(name.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }

    private ItemStack buildFiller(FileConfiguration cfg, String section, Material fallbackMat, short fallbackData) {
        String matName = cfg.getString(section + ".material", fallbackMat.name());
        int data = cfg.getInt(section + ".data", fallbackData);
        String name = cfg.getString(section + ".name", " ");
        return new ItemBuilder(parseMaterial(matName, fallbackMat), 1, (short) data).name(name).build();
    }

    private void drawBorder(ItemStack border) {
        int size = inventory.getSize();
        int cols = 9;
        int rows = size / cols;
        for (int col = 0; col < cols; col++) {
            int top = col;
            int bottom = (rows - 1) * cols + col;
            if (inventory.getItem(top) == null)    inventory.setItem(top, border);
            if (inventory.getItem(bottom) == null) inventory.setItem(bottom, border);
        }
        for (int row = 0; row < rows; row++) {
            int left  = row * cols;
            int right = row * cols + (cols - 1);
            if (inventory.getItem(left) == null)  inventory.setItem(left, border);
            if (inventory.getItem(right) == null) inventory.setItem(right, border);
        }
    }
}
