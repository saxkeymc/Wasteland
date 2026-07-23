package dev.r3faced.minecurse.wasteland.managers;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import dev.r3faced.minecurse.wasteland.api.event.WastelandToolGiveEvent;
import dev.r3faced.minecurse.wasteland.model.PlayerData;
import dev.r3faced.minecurse.wasteland.model.SkillType;
import dev.r3faced.minecurse.wasteland.nbt.NbtUtil;
import dev.r3faced.minecurse.wasteland.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ToolManager {

    private final WastelandPlugin plugin;

    public ToolManager(WastelandPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
    }

    public ItemStack buildOmniTool(SkillType skill, PlayerData data) {
        FileConfiguration cfg = plugin.getConfigManager().getTools();
        String path = "tools." + skill.getKey();

        String matName = cfg.getString(path + ".material", "DIAMOND_PICKAXE");
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.DIAMOND_PICKAXE;
            plugin.getLogger().warning("Invalid material '" + matName + "' for tool " + skill.getKey() + ", defaulting.");
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = MessageUtil.colorize(cfg.getString(path + ".name", skill.getKey()))
                .replace("{tier}",  String.valueOf(data.getTier()))
                .replace("{level}", String.valueOf(data.getLevel(skill)));
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList(path + ".lore")) {
            lore.add(MessageUtil.colorize(line)
                    .replace("{tier}",  String.valueOf(data.getTier()))
                    .replace("{level}", String.valueOf(data.getLevel(skill))));
        }
        meta.setLore(lore);

        if (cfg.isConfigurationSection(path + ".enchants")) {
            for (String enchName : cfg.getConfigurationSection(path + ".enchants").getKeys(false)) {
                int level = cfg.getInt(path + ".enchants." + enchName, 1);
                Enchantment enchant = Enchantment.getByName(enchName.toUpperCase());
                if (enchant != null) {
                    meta.addEnchant(enchant, level, true);
                } else {
                    plugin.getLogger().warning("Unknown enchantment: " + enchName);
                }
            }
        }

        if (cfg.getBoolean(path + ".hide-flags", false)) {
            try {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            } catch (Exception ignored) {}
        }

        int customModelData = cfg.getInt(path + ".custom-model-data", -1);
        if (customModelData > 0) {
            try {
                meta.getClass().getMethod("setCustomModelData", Integer.class)
                        .invoke(meta, customModelData);
            } catch (Exception ignored) {}
        }

        item.setItemMeta(meta);

        String nbtKey   = cfg.getString(path + ".nbt-key",   "wasteland_tool");
        String nbtValue = cfg.getString(path + ".nbt-value", skill.getKey());
        item = NbtUtil.setTag(item, nbtKey, nbtValue);

        return item;
    }

    public void giveOmniTool(Player player, SkillType skill) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        for (ItemStack item : player.getInventory().getContents()) {
            if (isOmniTool(item, skill)) {
                String msg = MessageUtil.getMessage(plugin, "tool.already-have");
                player.sendMessage(msg);
                return;
            }
        }

        ItemStack tool = buildOmniTool(skill, data);
        WastelandToolGiveEvent event = new WastelandToolGiveEvent(player, skill, tool);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        player.getInventory().addItem(event.getItem());

        String msg = MessageUtil.getMessage(plugin, "tool.received")
                .replace("{skill}", skill.getKey());
        player.sendMessage(msg);
    }

    public boolean isOmniTool(ItemStack item, SkillType skill) {
        if (item == null || item.getType() == Material.AIR) return false;
        FileConfiguration cfg = plugin.getConfigManager().getTools();
        String nbtKey = cfg.getString("tools." + skill.getKey() + ".nbt-key", "wasteland_tool");
        String nbtExpected = cfg.getString("tools." + skill.getKey() + ".nbt-value", skill.getKey());
        String nbtActual = NbtUtil.getTag(item, nbtKey);
        return nbtExpected.equals(nbtActual);
    }

    public SkillType getToolSkill(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        for (SkillType skill : SkillType.values()) {
            if (isOmniTool(item, skill)) return skill;
        }
        return null;
    }

    public SkillType getSkillForWorld(String worldName) {
        FileConfiguration cfg = plugin.getConfigManager().getMainConfig();
        for (SkillType skill : SkillType.values()) {
            boolean enabled = cfg.getBoolean("worlds." + skill.getKey() + ".enabled", false);
            String configuredWorld = cfg.getString("worlds." + skill.getKey() + ".world", "");
            if (enabled && configuredWorld.equalsIgnoreCase(worldName)) {
                return skill;
            }
        }
        return null;
    }
}
