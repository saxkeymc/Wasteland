package dev.r3faced.minecurse.wasteland.config;

import dev.r3faced.minecurse.wasteland.WastelandPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final WastelandPlugin plugin;

    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration skillsConfig;
    private FileConfiguration tiersConfig;
    private FileConfiguration guiConfig;
    private FileConfiguration toolsConfig;
    private FileConfiguration helpConfig;
    private FileConfiguration commandsConfig;

    public ConfigManager(WastelandPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();

        messagesConfig = loadConfig("messages.yml");
        skillsConfig   = loadConfig("skills.yml");
        tiersConfig    = loadConfig("tiers.yml");
        guiConfig      = loadConfig("gui.yml");
        toolsConfig    = loadConfig("tools.yml");
        helpConfig     = loadConfig("help.yml");
        commandsConfig = loadConfig("commands.yml");
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    public FileConfiguration getSkills() {
        return skillsConfig;
    }

    public FileConfiguration getTiers() {
        return tiersConfig;
    }

    public FileConfiguration getGui() {
        return guiConfig;
    }

    public FileConfiguration getTools() {
        return toolsConfig;
    }

    public FileConfiguration getHelp() {
        return helpConfig;
    }

    public FileConfiguration getCommandsConfig() {
        return commandsConfig;
    }
}
