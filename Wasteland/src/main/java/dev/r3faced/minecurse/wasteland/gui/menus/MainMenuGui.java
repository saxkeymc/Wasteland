package dev.r3faced.minecurse.wasteland.gui.menus;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.gui.WastelandGui;
import dev.r3faced.minecurse.wasteland.managers.TeleportManager;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.utils.ItemBuilder;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import dev.r3faced.minecurse.wasteland.utils.PlaytimeFormatter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The main Wasteland menu opened by /wasteland.
 * <p>
 * Layout (54 slots):
 * <pre>
 *  0  1  2  3  4  5  6  7  8
 *  9 10 11 12 13 14 15 16 17
 * 18 19 20 21 22 23 24 25 26     ← Mining(20), Chopping(21), Farming(23), Fishing(24)
 * 27 28 29 30 31 32 33 34 35
 * 36 37 38 39 40 41 42 43 44
 * 45 46 47 48 49 50 51 52 53     ← Close(49), Profile head(53)
 * </pre>
 * <p>
 * Clicking a skill button ONLY teleports the player to the configured
 * destination for that skill. It does NOT open another GUI. The profile
 * head in the bottom-right shows the player's skin, name, and Wasteland
 * statistics including playtime.
 */
public class MainMenuGui extends WastelandGui {

    public MainMenuGui(WastelandPlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        String title = MessageUtil.colorize(cfg.getString("main-menu.title", "&7Wasteland"));
        int size = cfg.getInt("main-menu.size", 54);
        createInventory(title, size);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // ── Border glass panes ────────────────────────────────────────────────
        ItemStack borderFiller = buildFiller(cfg, "main-menu.border-item", Material.STAINED_GLASS_PANE, (short) 15);
        ItemStack innerFiller  = buildFiller(cfg, "main-menu.fill-item",   Material.STAINED_GLASS_PANE, (short) 7);
        fill(innerFiller);
        drawBorder(borderFiller);

        // ── Skill buttons ─────────────────────────────────────────────────────
        setSkillButton(cfg, data, "mining",      SkillType.MINING);
        setSkillButton(cfg, data, "woodcutting", SkillType.WOODCUTTING);
        setSkillButton(cfg, data, "farming",     SkillType.FARMING);
        setSkillButton(cfg, data, "fishing",     SkillType.FISHING);

        // ── Close button ──────────────────────────────────────────────────────
        int closeSlot = cfg.getInt("main-menu.buttons.close.slot", 49);
        Material closeMat = parseMaterial(cfg.getString("main-menu.buttons.close.material", "BARRIER"), Material.BARRIER);
        String closeName = cfg.getString("main-menu.buttons.close.name", "&c&lClose");
        ItemStack closeItem = new ItemBuilder(closeMat)
                .name(closeName)
                .lore(cfg.getStringList("main-menu.buttons.close.lore"))
                .build();
        setItem(closeSlot, closeItem);

        // ── Player profile head (bottom-right) ────────────────────────────────
        setProfileHead(cfg, data);
    }

    private void setSkillButton(FileConfiguration cfg, PlayerData data, String cfgKey, SkillType skill) {
        int slot = cfg.getInt("main-menu.buttons." + cfgKey + ".slot", defaultSkillSlot(skill));
        Material defaultMat = defaultSkillMaterial(skill);
        Material mat = parseMaterial(cfg.getString("main-menu.buttons." + cfgKey + ".material", defaultMat.name()), defaultMat);
        int dataVal = cfg.getInt("main-menu.buttons." + cfgKey + ".data", 0);
        String name = cfg.getString("main-menu.buttons." + cfgKey + ".name", "&f" + cfgKey);
        List<String> lore = applyLore(cfg.getStringList("main-menu.buttons." + cfgKey + ".lore"), data, skill);
        ItemStack item = new ItemBuilder(mat, 1, (short) dataVal).name(name).lore(lore).build();
        setItem(slot, item);
    }

    private int defaultSkillSlot(SkillType skill) {
        switch (skill) {
            case MINING:       return 20;
            case WOODCUTTING:  return 21;
            case FARMING:      return 23;
            case FISHING:      return 24;
            default:           return 22;
        }
    }

    private Material defaultSkillMaterial(SkillType skill) {
        switch (skill) {
            case MINING:       return Material.DIAMOND_PICKAXE;
            case WOODCUTTING:  return Material.DIAMOND_AXE;
            case FARMING:      return Material.DIAMOND_HOE;
            case FISHING:      return Material.FISHING_ROD;
            default:           return Material.STONE;
        }
    }

    private List<String> applyLore(List<String> raw, PlayerData data, SkillType skill) {
        List<String> result = new ArrayList<>();
        for (String line : raw) {
            String l = MessageUtil.colorize(line);
            l = l.replace("{" + skill.getKey() + "_level}", String.valueOf(data.getLevel(skill)));
            l = l.replace("{" + skill.getKey() + "_xp}",    String.valueOf(data.getXp(skill)));
            for (SkillType s : SkillType.values()) {
                l = l.replace("{" + s.getKey() + "_level}", String.valueOf(data.getLevel(s)));
                l = l.replace("{" + s.getKey() + "_xp}",    String.valueOf(data.getXp(s)));
            }
            l = l.replace("{total_level}", String.valueOf(data.getTotalLevel()));
            l = l.replace("{total_xp}",    String.valueOf(data.getTotalXp()));
            result.add(l);
        }
        return result;
    }

    private void setProfileHead(FileConfiguration cfg, PlayerData data) {
        int slot = cfg.getInt("main-menu.buttons.profile.slot", 53);
        String name = cfg.getString("main-menu.buttons.profile.name", "&a&l{player}");
        name = MessageUtil.colorize(name.replace("{player}", player.getName()));

        String playtime = PlaytimeFormatter.format(plugin, data.getPlaytimeSeconds());

        List<String> loreRaw = cfg.getStringList("main-menu.buttons.profile.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreRaw) {
            String l = MessageUtil.colorize(line);
            for (SkillType s : SkillType.values()) {
                l = l.replace("{" + s.getKey() + "_level}", String.valueOf(data.getLevel(s)));
                l = l.replace("{" + s.getKey() + "_xp}",    String.valueOf(data.getXp(s)));
            }
            l = l.replace("{total_level}", String.valueOf(data.getTotalLevel()));
            l = l.replace("{total_xp}",    String.valueOf(data.getTotalXp()));
            l = l.replace("{tier}",        String.valueOf(data.getTier()));
            l = l.replace("{playtime}",    playtime);
            l = l.replace("{player}",      player.getName());
            lore.add(l);
        }

        ItemStack skull = createPlayerHead(player, name, lore);
        setItem(slot, skull);
    }

    /** Build a player-head ItemStack that shows the player's skin in 1.8.8. */
    @SuppressWarnings("deprecation")
    private ItemStack createPlayerHead(Player player, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = skull.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            try {
                skullMeta.setOwner(player.getName());
            } catch (Exception ignored) {}
        }
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        FileConfiguration cfg = plugin.getConfigManager().getGui();
        int slot = event.getSlot();

        if (slot == cfg.getInt("main-menu.buttons.mining.slot", 20)) {
            handleSkillClick(SkillType.MINING);
        } else if (slot == cfg.getInt("main-menu.buttons.woodcutting.slot", 21)) {
            handleSkillClick(SkillType.WOODCUTTING);
        } else if (slot == cfg.getInt("main-menu.buttons.farming.slot", 23)) {
            handleSkillClick(SkillType.FARMING);
        } else if (slot == cfg.getInt("main-menu.buttons.fishing.slot", 24)) {
            handleSkillClick(SkillType.FISHING);
        } else if (slot == cfg.getInt("main-menu.buttons.close.slot", 49)) {
            player.closeInventory();
        }
    }

    /**
     * Clicking a skill button ONLY teleports the player to the configured
     * destination. No menu is opened. If no destination is set, a
     * configurable 'not configured' message is sent.
     */
    private void handleSkillClick(SkillType skill) {
        TeleportManager tm = plugin.getTeleportManager();
        if (tm.hasTeleport(skill)) {
            org.bukkit.Location dest = tm.getTeleport(skill);
            if (dest != null) {
                player.teleport(dest);
                String msg = MessageUtil.getMessage(plugin, "teleport.teleported")
                        .replace("{skill}", skill.getKey());
                player.sendMessage(msg);
            } else {
                player.sendMessage(MessageUtil.getMessage(plugin, "teleport.not-configured")
                        .replace("{skill}", skill.getKey()));
            }
        } else {
            player.sendMessage(MessageUtil.getMessage(plugin, "teleport.not-configured")
                    .replace("{skill}", skill.getKey()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private ItemStack buildFiller(FileConfiguration cfg, String section, Material fallbackMat, short fallbackData) {
        String matName = cfg.getString(section + ".material", fallbackMat.name());
        int data = cfg.getInt(section + ".data", fallbackData);
        String name = cfg.getString(section + ".name", " ");
        Material mat = parseMaterial(matName, fallbackMat);
        return new ItemBuilder(mat, 1, (short) data).name(name).build();
    }

    /**
     * Draw a 1-slot-thick border around the inventory edges. Only fills
     * slots that are currently empty/null so existing skill buttons remain
     * untouched if they happen to sit on the border.
     */
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
