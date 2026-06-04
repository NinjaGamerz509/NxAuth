package dev.nxauth;

import dev.nxauth.auth.AuthManager;
import dev.nxauth.auth.SessionManager;
import dev.nxauth.antibot.AntiBotManager;
import dev.nxauth.backup.BackupManager;
import dev.nxauth.commands.*;
import dev.nxauth.config.ConfigManager;
import dev.nxauth.config.MessageManager;
import dev.nxauth.database.DatabaseManager;
import dev.nxauth.listeners.*;
import dev.nxauth.maintenance.MaintenanceManager;
import dev.nxauth.notifications.AdminAlertManager;
import dev.nxauth.notifications.DiscordWebhook;
import dev.nxauth.premium.PremiumManager;
import dev.nxauth.security.BruteForceProtection;
import dev.nxauth.ui.BossBarManager;
import dev.nxauth.ui.MOTDManager;
import dev.nxauth.web.WebServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class NxAuth extends JavaPlugin {

    private static NxAuth instance;

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private SessionManager sessionManager;
    private BruteForceProtection bruteForceProtection;
    private AntiBotManager antiBotManager;
    private BossBarManager bossBarManager;
    private MOTDManager motdManager;
    private MaintenanceManager maintenanceManager;
    private AdminAlertManager adminAlertManager;
    private DiscordWebhook discordWebhook;
    private BackupManager backupManager;
    private PremiumManager premiumManager;
    private WebServer webServer;
    private dev.nxauth.auth.CaptchaManager captchaManager;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        // Create plugin folders
        createFolders();

        // Load configs
        getLogger().info("Loading configuration...");
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);

        // Initialize database
        getLogger().info("Initializing database...");
        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        getLogger().info("Starting managers...");
        sessionManager = new SessionManager(this);
        bruteForceProtection = new BruteForceProtection(this);
        authManager = new AuthManager(this);
        antiBotManager = new AntiBotManager(this);
        bossBarManager = new BossBarManager(this);
        motdManager = new MOTDManager(this);
        maintenanceManager = new MaintenanceManager(this);
        adminAlertManager = new AdminAlertManager(this);
        discordWebhook = new DiscordWebhook(this);
        backupManager = new BackupManager(this);
        premiumManager = new PremiumManager(this);
        captchaManager = new dev.nxauth.auth.CaptchaManager(this);

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Start web dashboard
        if (configManager.isWebDashboardEnabled()) {
            getLogger().info("Starting web dashboard...");
            webServer = new WebServer(this);
            webServer.start();
            getLogger().info("Web dashboard started on port " + configManager.getWebPort());
        }

        // Schedule tasks
        scheduleTasks();

        getLogger().info("NxAuth v" + getDescription().getVersion() + " enabled successfully!");
        getLogger().info("Players can now login and register securely.");
    }

    @Override
    public void onDisable() {
        // Stop web server
        if (webServer != null) webServer.stop();

        // Save all sessions
        if (sessionManager != null) sessionManager.saveAll();

        // Close database
        if (databaseManager != null) databaseManager.close();

        // Remove all bossbars
        if (bossBarManager != null) bossBarManager.removeAll();

        getLogger().info("NxAuth disabled. Goodbye!");
    }

    private void createFolders() {
        String[] folders = {"backups", "logs", "icons", "ssl"};
        for (String folder : folders) {
            File f = new File(getDataFolder(), folder);
            if (!f.exists()) f.mkdirs();
        }
        saveDefaultConfig();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerPingListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
        getLogger().info("Registered " + 7 + " event listeners.");
    }

    private void registerCommands() {
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("logout").setExecutor(new LogoutCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("language").setExecutor(new LanguageCommand(this));
        getCommand("2fa").setExecutor(new TwoFACommand(this));
        getCommand("nxauth").setExecutor(new AdminCommand(this));
        getCommand("nxauth").setTabCompleter(new AdminCommand(this));
        getLogger().info("Registered " + 7 + " commands.");
    }

    private void scheduleTasks() {
        // Auto-logout AFK check (every 30 seconds)
        int afkMinutes = configManager.getAfkLogoutMinutes();
        if (afkMinutes > 0) {
            getServer().getScheduler().runTaskTimer(this,
                () -> sessionManager.checkAfkLogout(), 600L, 600L);
        }

        // Auto backup
        if (configManager.isBackupEnabled()) {
            long intervalTicks = configManager.getBackupIntervalHours() * 72000L;
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> backupManager.createBackup(), intervalTicks, intervalTicks);
        }

        // BossBar animation
        getServer().getScheduler().runTaskTimer(this,
            () -> bossBarManager.tick(), 0L, configManager.getBossBarAnimationSpeed());

        // Discord daily stats
        if (configManager.isDiscordEnabled()) {
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> discordWebhook.sendDailyStats(), 72000L, 1728000L);
        }

        // Anti-bot cleanup
        getServer().getScheduler().runTaskTimer(this,
            () -> antiBotManager.cleanup(), 1200L, 1200L);
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
        motdManager.reload();
    }

    private void printBanner() {
        getLogger().info("  _   _      _         _   _   _   _   ");
        getLogger().info(" | \\ | |_  _/ \\  _   _| |_| |_| | | |  ");
        getLogger().info(" |  \\| \\ \\/ / _ \\| | | |  _|  _| |_| |  ");
        getLogger().info(" | |\\  |>  < (_) | |_| | |_| |_|   _|  ");
        getLogger().info(" |_| \\_/_/\\_\\___/ \\__,_|\\__|\\__|_| |_|  ");
        getLogger().info("  Ultimate Auth Plugin v" + getDescription().getVersion());
        getLogger().info("  Made with ❤ by NxAuth Team");
        getLogger().info("  ==========================================");
    }

    // =================== GETTERS ===================
    public static NxAuth getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuthManager getAuthManager() { return authManager; }
    public SessionManager getSessionManager() { return sessionManager; }
    public BruteForceProtection getBruteForceProtection() { return bruteForceProtection; }
    public AntiBotManager getAntiBotManager() { return antiBotManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public MOTDManager getMotdManager() { return motdManager; }
    public MaintenanceManager getMaintenanceManager() { return maintenanceManager; }
    public AdminAlertManager getAdminAlertManager() { return adminAlertManager; }
    public DiscordWebhook getDiscordWebhook() { return discordWebhook; }
    public BackupManager getBackupManager() { return backupManager; }
    public PremiumManager getPremiumManager() { return premiumManager; }
    public WebServer getWebServer() { return webServer; }
    public dev.nxauth.auth.CaptchaManager getCaptchaManager() { return captchaManager; }
}
