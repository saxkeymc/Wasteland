package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.managers.TierManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Tier browser menu showing all 5 tiers as colored glass panes.
 * <p>
 * There is only ONE shared tier progression per player, so this menu
 * does not take a SkillType parameter. Clicking a tier opens the reward
 * list for that tier if the player has unlocked it; otherwise a
 * configurable unlock-failed message is sent listing which skills
 * still need leveling.
 * <p>
 * Opened primarily via /wasteland tiers.
 */
public class TierMenuGui extends WastelandGui {

    public TierMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        String title = MessageUtil.colorize(cfg.getString("tier-menu.title",
                "&7Wasteland &8\u2022 &aTier Rewards"));
        int size = cfg.getInt("tier-menu.size", 27);
        createInventory(title, size);

        ItemStack border = buildFiller(cfg, "tier-menu.border-item", Material.STAINED_GLASS_PANE, (short) 15);
        ItemStack filler = buildFiller(cfg, "tier-menu.fill-item",   Material.STAINED_GLASS_PANE, (short) 7);
        fill(filler);
        drawBorder(border);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        for (int tier = 1; tier <= TierManager.TIER_COUNT; tier++) {
            int slot = cfg.getInt("tier-menu.tier-slots." + tier, 10 + tier);
            ItemStack glass = buildTierGlass(cfg, tier, data);
            setItem(slot, glass);
        }

        // Back button (returns to main menu)
        int backSlot = cfg.getInt("tier-menu.back.slot", 22);
        Material backMat = parseMaterial(cfg.getString("tier-menu.back.material", "ARROW"), Material.ARROW);
        String backName = MessageUtil.colorize(cfg.getString("tier-menu.back.name", "&7\u00ab Back"));
        setItem(backSlot, new ItemBuilder(backMat).name(backName).build());
    }

    private ItemStack buildTierGlass(FileConfiguration cfg, int tier, PlayerData data) {
        FileConfiguration tiersCfg = plugin.getConfigManager().getTiers();
        String displayName = tiersCfg.getString("tiers." + tier + ".display-name", "&fTier " + tier);
        String material = tiersCfg.getString("tiers." + tier + ".glass-color", "STAINED_GLASS_PANE");
        int glassData = tiersCfg.getInt("tiers." + tier + ".glass-data", 0);
        int required = plugin.getTierManager().getRequiredLevel(tier);
        // Tier is "unlocked" only if EVERY skill has reached the required level.
        boolean unlocked = plugin.getTierManager().meetsRequirements(data, tier);
        boolean claimed  = data.hasClaimedTier(tier);

        FileConfiguration guiCfg = plugin.getConfigManager().getGui();
        String loreText = MessageUtil.colorize(guiCfg.getString("tier-menu.tier-lore.click-text",
                "&fClick here to view this tier's rewards!"));
        String statusLine;
        if (claimed) {
            statusLine = MessageUtil.colorize(guiCfg.getString("tier-menu.tier-lore.claimed", "&a\u2714 Claimed"));
        } else if (unlocked) {
            statusLine = MessageUtil.colorize(guiCfg.getString("tier-menu.tier-lore.unlocked", "&aUnlocked!"));
        } else {
            String tmpl = guiCfg.getString("tier-menu.tier-lore.locked", "&cRequires every skill at Level {level}");
            statusLine = MessageUtil.colorize(tmpl.replace("{level}", String.valueOf(required)));
        }

        return new ItemBuilder(parseMaterial(material, Material.STAINED_GLASS_PANE), 1, (short) glassData)
                .name(displayName)
                .lore(loreText, statusLine)
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        for (int tier = 1; tier <= TierManager.TIER_COUNT; tier++) {
            int tierSlot = cfg.getInt("tier-menu.tier-slots." + tier, 10 + tier);
            // Both LEFT CLICK and RIGHT CLICK perform the same action:
            // open that tier's reward preview GUI (if unlocked).
            if (slot == tierSlot) {
                PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (plugin.getTierManager().meetsRequirements(data, tier)) {
                    new RewardPageMenuGui(plugin, player, tier, 0).open();
                } else {
                    // Lock the inventory by closing it and sending the
                    // configurable unlock-failed message.
                    player.closeInventory();
                    plugin.getTierManager().sendUnlockFailedMessage(player, tier);
                }
                return;
            }
        }

        if (slot == cfg.getInt("tier-menu.back.slot", 22)) {
            new MainMenuGui(plugin, player).open();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
