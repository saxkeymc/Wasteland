package dev.r3faced.minecurse.wasteland;

import dev.r3faced.minecurse.wasteland.commands.WastelandCommand;
import dev.r3faced.minecurse.wasteland.api.WastelandApi;
import dev.r3faced.minecurse.wasteland.api.WastelandProvider;
import dev.r3faced.minecurse.wasteland.api.internal.WastelandApiImpl;
import dev.r3faced.minecurse.wasteland.config.ConfigManager;
import dev.r3faced.minecurse.wasteland.data.DataManager;
import dev.r3faced.minecurse.wasteland.data.mysql.MySQLDataManager;
import dev.r3faced.minecurse.wasteland.data.yaml.YamlDataManager;
import dev.r3faced.minecurse.wasteland.gui.GuiListener;
import dev.r3faced.minecurse.wasteland.listeners.FishingListener;
import dev.r3faced.minecurse.wasteland.listeners.HarvestListener;
import dev.r3faced.minecurse.wasteland.listeners.MiningListener;
import dev.r3faced.minecurse.wasteland.listeners.WoodcuttingListener;
import dev.r3faced.minecurse.wasteland.listeners.WorldChangeListener;
import dev.r3faced.minecurse.wasteland.managers.SkillManager;
import dev.r3faced.minecurse.wasteland.managers.TierManager;
import dev.r3faced.minecurse.wasteland.managers.ToolManager;
import dev.r3faced.minecurse.wasteland.placeholders.WastelandExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class WastelandPlugin extends JavaPlugin {

    private static WastelandPlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private SkillManager skillManager;
    private TierManager tierManager;
    private ToolManager toolManager;
    private dev.r3faced.minecurse.wasteland.managers.TeleportManager teleportManager;
    private dev.r3faced.minecurse.wasteland.managers.PlaytimeTask playtimeTask;
    private dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager wastelandWorldManager;
    private dev.r3faced.minecurse.wasteland.editor.PreviewRewardEditor previewRewardEditor;
    private dev.r3faced.minecurse.wasteland.listeners.MiningListener miningListener;
    private dev.r3faced.minecurse.wasteland.listeners.CommandWhitelistListener commandWhitelistListener;
    private dev.r3faced.minecurse.wasteland.managers.TierLockManager tierLockManager;
    private dev.r3faced.minecurse.wasteland.managers.WastelandArmorManager armorManager;
    private dev.r3faced.minecurse.wasteland.managers.StartDateManager startDateManager;
    private dev.r3faced.minecurse.wasteland.managers.DustManager dustManager;
    private dev.r3faced.minecurse.wasteland.managers.FakeBlockManager fakeBlockManager;
    private dev.r3faced.minecurse.wasteland.managers.PvpZoneManager pvpZoneManager;
    private WastelandApi api;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("skills.yml", false);
        saveResource("tiers.yml", false);
        saveResource("gui.yml", false);
        saveResource("tools.yml", false);
        saveResource("teleports.yml", false);
        saveResource("help.yml", false);
        saveResource("commands.yml", false);

        configManager = new ConfigManager(this);
        configManager.loadAll();

        tierLockManager = new dev.r3faced.minecurse.wasteland.managers.TierLockManager(this);
        armorManager = new dev.r3faced.minecurse.wasteland.managers.WastelandArmorManager(this);
        startDateManager = new dev.r3faced.minecurse.wasteland.managers.StartDateManager(this);
        dustManager = new dev.r3faced.minecurse.wasteland.managers.DustManager(this);
        fakeBlockManager = new dev.r3faced.minecurse.wasteland.managers.FakeBlockManager();
        pvpZoneManager = new dev.r3faced.minecurse.wasteland.managers.PvpZoneManager(this);

        String storageType = configManager.getMainConfig().getString("storage.type", "YAML").toUpperCase();
        if (storageType.equals("MYSQL")) {
            dataManager = new MySQLDataManager(this);
        } else {
            dataManager = new YamlDataManager(this);
        }
        dataManager.init();

        skillManager = new SkillManager(this);
        tierManager = new TierManager(this);
        toolManager = new ToolManager(this);
        teleportManager = new dev.r3faced.minecurse.wasteland.managers.TeleportManager(this);
        wastelandWorldManager = new dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager(this);

        api = new WastelandApiImpl(this);
        WastelandProvider.register(api);
        Bukkit.getServicesManager().register(WastelandApi.class, api, this, ServicePriority.Normal);

        getCommand("wasteland").setExecutor(new WastelandCommand(this));
        getCommand("wasteland").setTabCompleter(new WastelandCommand(this));

        dev.r3faced.minecurse.wasteland.commands.SkillSwitchCommand skillSwitch =
                new dev.r3faced.minecurse.wasteland.commands.SkillSwitchCommand(this);
        getCommand("mining").setExecutor(skillSwitch);
        getCommand("chopping").setExecutor(skillSwitch);
        getCommand("farming").setExecutor(skillSwitch);
        getCommand("fishing").setExecutor(skillSwitch);

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        miningListener = new MiningListener(this);
        Bukkit.getPluginManager().registerEvents(miningListener, this);
        Bukkit.getPluginManager().registerEvents(new WoodcuttingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HarvestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new FishingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.FishingMinigameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldChangeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.WastelandWorldProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.ItemDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.ToolRightClickListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.FakeBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.EnchantCancelListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.FallTeleportListener(this), this);
        Bukkit.getPluginManager().registerEvents(new dev.r3faced.minecurse.wasteland.listeners.DeathLootListener(this), this);
        previewRewardEditor = new dev.r3faced.minecurse.wasteland.editor.PreviewRewardEditor(this);
        Bukkit.getPluginManager().registerEvents(previewRewardEditor, this);
        commandWhitelistListener = new dev.r3faced.minecurse.wasteland.listeners.CommandWhitelistListener(this);
        Bukkit.getPluginManager().registerEvents(commandWhitelistListener, this);

        playtimeTask = new dev.r3faced.minecurse.wasteland.managers.PlaytimeTask(this);
        playtimeTask.start();

        new dev.r3faced.minecurse.wasteland.managers.XpBarTask(this).start();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WastelandExpansion(this).canRegister();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        getLogger().info("Wasteland v" + getDescription().getVersion() + " has been enabled.");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            Bukkit.getServicesManager().unregister(WastelandApi.class, api);
            WastelandProvider.unregister(api);
        }

        if (playtimeTask != null) {
            try { playtimeTask.cancel(); } catch (IllegalStateException ignored) {}
        }

        if (dataManager != null) {
            try {
                Bukkit.getOnlinePlayers().forEach(p -> dataManager.savePlayerSync(p.getUniqueId()));
            } catch (Throwable t) {
                getLogger().severe("Error during synchronous player data save: " + t.getMessage());
            }
            try {
                dataManager.shutdown();
            } catch (Throwable t) {
                getLogger().severe("Error during data manager shutdown: " + t.getMessage());
            }
        }
        getLogger().info("Wasteland has been disabled.");
    }

    public void reload() {
        if (dataManager != null) {
            Bukkit.getOnlinePlayers().forEach(p -> dataManager.savePlayerSync(p.getUniqueId()));
        }

        configManager.loadAll();
        skillManager.reload();
        tierManager.reload();
        toolManager.reload();
        if (teleportManager != null) {
            teleportManager.reload();
        }
        if (wastelandWorldManager != null) {
            wastelandWorldManager.reload();
        }
        if (commandWhitelistListener != null) {
            commandWhitelistListener.reload();
        }
        if (tierLockManager != null) {
            tierLockManager.reload();
        }
    }

    public dev.r3faced.minecurse.wasteland.managers.TeleportManager getTeleportManager() {
        if (teleportManager == null) {
            teleportManager = new dev.r3faced.minecurse.wasteland.managers.TeleportManager(this);
        }
        return teleportManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager getWastelandWorldManager() {
        if (wastelandWorldManager == null) {
            wastelandWorldManager = new dev.r3faced.minecurse.wasteland.managers.WastelandWorldManager(this);
        }
        return wastelandWorldManager;
    }

    public dev.r3faced.minecurse.wasteland.editor.PreviewRewardEditor getPreviewRewardEditor() {
        if (previewRewardEditor == null) {
            previewRewardEditor = new dev.r3faced.minecurse.wasteland.editor.PreviewRewardEditor(this);
        }
        return previewRewardEditor;
    }

    public dev.r3faced.minecurse.wasteland.managers.TierLockManager getTierLockManager() {
        if (tierLockManager == null) {
            tierLockManager = new dev.r3faced.minecurse.wasteland.managers.TierLockManager(this);
        }
        return tierLockManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.WastelandArmorManager getArmorManager() {
        if (armorManager == null) {
            armorManager = new dev.r3faced.minecurse.wasteland.managers.WastelandArmorManager(this);
        }
        return armorManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.StartDateManager getStartDateManager() {
        if (startDateManager == null) {
            startDateManager = new dev.r3faced.minecurse.wasteland.managers.StartDateManager(this);
        }
        return startDateManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.DustManager getDustManager() {
        if (dustManager == null) {
            dustManager = new dev.r3faced.minecurse.wasteland.managers.DustManager(this);
        }
        return dustManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.FakeBlockManager getFakeBlockManager() {
        if (fakeBlockManager == null) {
            fakeBlockManager = new dev.r3faced.minecurse.wasteland.managers.FakeBlockManager();
        }
        return fakeBlockManager;
    }

    public dev.r3faced.minecurse.wasteland.managers.PvpZoneManager getPvpZoneManager() {
        if (pvpZoneManager == null) {
            pvpZoneManager = new dev.r3faced.minecurse.wasteland.managers.PvpZoneManager(this);
        }
        return pvpZoneManager;
    }

    public static WastelandPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public TierManager getTierManager() {
        return tierManager;
    }

    public ToolManager getToolManager() {
        return toolManager;
    }

    public WastelandApi getApi() {
        return api;
    }
}
