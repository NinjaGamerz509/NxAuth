package dev.nxauth.config;

import dev.nxauth.NxAuth;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final NxAuth plugin;
    private FileConfiguration config;

    public ConfigManager(NxAuth plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ========== DATABASE ==========
    public String getDbType() { return config.getString("database.type", "sqlite"); }
    public String getSqliteFile() { return config.getString("database.sqlite.file", "nxauth.db"); }
    public String getMysqlHost() { return config.getString("database.mysql.host", "localhost"); }
    public int getMysqlPort() { return config.getInt("database.mysql.port", 3306); }
    public String getMysqlDatabase() { return config.getString("database.mysql.database", "nxauth"); }
    public String getMysqlUsername() { return config.getString("database.mysql.username", "root"); }
    public String getMysqlPassword() { return config.getString("database.mysql.password", ""); }
    public int getMysqlPoolSize() { return config.getInt("database.mysql.pool-size", 10); }

    // ========== AUTH ==========
    public int getLoginTimeout() { return config.getInt("auth.login-timeout", 60); }
    public int getMaxAttempts() { return config.getInt("auth.max-attempts", 5); }
    public int getTempbanDuration() { return config.getInt("auth.tempban-duration", 30); }
    public int getAfkLogoutMinutes() { return config.getInt("auth.afk-logout", 10); }
    public boolean isIpSessionEnabled() { return config.getBoolean("auth.ip-session", true); }
    public int getIpSessionDuration() { return config.getInt("auth.ip-session-duration", 24); }
    public int getMinPasswordLength() { return config.getInt("auth.min-password-length", 6); }
    public int getMaxPasswordLength() { return config.getInt("auth.max-password-length", 32); }
    public boolean isBlockWeakPasswords() { return config.getBoolean("auth.block-weak-passwords", true); }
    public boolean isAllowMultiplePerIp() { return config.getBoolean("auth.allow-multiple-per-ip", true); }
    public int getMaxAccountsPerIp() { return config.getInt("auth.max-accounts-per-ip", 3); }

    // ========== PREMIUM / BEDROCK ==========
    public boolean isPremiumEnabled() { return config.getBoolean("premium.enabled", true); }
    public boolean isBedrockAutoLogin() { return config.getBoolean("premium.bedrock-auto-login", true); }
    public String getBedrockPrefix() { return config.getString("premium.bedrock-prefix", "."); }

    // ========== CAPTCHA ==========
    public boolean isCaptchaEnabled() { return config.getBoolean("captcha.enabled", true); }
    public String getCaptchaType() { return config.getString("captcha.type", "math"); }
    public String getCaptchaShowOn() { return config.getString("captcha.show-on", "register"); }
    public int getCaptchaTimeout() { return config.getInt("captcha.timeout", 30); }
    public int getCaptchaMaxFails() { return config.getInt("captcha.max-fails", 3); }

    // ========== 2FA ==========
    public boolean is2faEnabled() { return config.getBoolean("two-factor-auth.enabled", true); }
    public boolean isForce2faForAdmins() { return config.getBoolean("two-factor-auth.force-for-admins", true); }
    public boolean isPlayerToggle2fa() { return config.getBoolean("two-factor-auth.player-toggle", true); }
    public int getBackupCodesCount() { return config.getInt("two-factor-auth.backup-codes-count", 8); }

    // ========== JOIN SCREEN ==========
    public boolean isJoinScreenEnabled() { return config.getBoolean("join-screen.enabled", true); }
    public boolean isAuthLocationEnabled() { return config.getBoolean("join-screen.auth-location.enabled", true); }
    public String getAuthWorld() { return config.getString("join-screen.auth-location.world", "world"); }
    public double getAuthX() { return config.getDouble("join-screen.auth-location.x", 0.5); }
    public double getAuthY() { return config.getDouble("join-screen.auth-location.y", 64.0); }
    public double getAuthZ() { return config.getDouble("join-screen.auth-location.z", 0.5); }
    public boolean isBlindnessEnabled() { return config.getBoolean("join-screen.effects.blindness", true); }
    public boolean isFreezeEnabled() { return config.getBoolean("join-screen.effects.freeze", true); }
    public boolean isHideInventory() { return config.getBoolean("join-screen.effects.hide-inventory", true); }
    public boolean isHideFromOthers() { return config.getBoolean("join-screen.effects.hide-from-others", true); }

    // ========== BOSSBAR ==========
    public boolean isBossBarEnabled() { return config.getBoolean("bossbar.enabled", true); }
    public boolean isBossBarAnimated() { return config.getBoolean("bossbar.animate", true); }
    public long getBossBarAnimationSpeed() { return config.getLong("bossbar.animation-speed", 10L); }

    // ========== SOUNDS ==========
    public boolean areSoundsEnabled() { return config.getBoolean("sounds.enabled", true); }
    public String getSoundLoginSuccess() { return config.getString("sounds.login-success", "ENTITY_PLAYER_LEVELUP"); }
    public String getSoundLoginFail() { return config.getString("sounds.login-fail", "ENTITY_VILLAGER_NO"); }
    public String getSoundRegisterSuccess() { return config.getString("sounds.register-success", "UI_TOAST_CHALLENGE_COMPLETE"); }
    public String getSoundRegisterFirst() { return config.getString("sounds.register-first-time", "UI_TOAST_CHALLENGE_COMPLETE"); }
    public String getSoundTimeoutWarning() { return config.getString("sounds.timeout-warning", "BLOCK_NOTE_BLOCK_BELL"); }
    public float getSoundVolume() { return (float) config.getDouble("sounds.volume", 1.0); }
    public float getSoundPitch() { return (float) config.getDouble("sounds.pitch", 1.0); }

    // ========== ANIMATIONS ==========
    public boolean isLoginParticlesEnabled() { return config.getBoolean("animations.login-particles.enabled", true); }
    public String getLoginParticleType() { return config.getString("animations.login-particles.type", "TOTEM_OF_UNDYING"); }
    public int getLoginParticleCount() { return config.getInt("animations.login-particles.count", 50); }
    public boolean isRegisterFireworkEnabled() { return config.getBoolean("animations.register-firework.enabled", true); }
    public boolean isCinematicEnabled() { return config.getBoolean("animations.login-cinematic.enabled", false); }
    public int getCinematicDuration() { return config.getInt("animations.login-cinematic.duration-ticks", 60); }

    // ========== MOTD ==========
    public boolean isMOTDEnabled() { return config.getBoolean("motd.enabled", true); }
    public String getMotdRegisteredLine1() { return config.getString("motd.registered.line1", "&aNxAuth Server"); }
    public String getMotdRegisteredLine2() { return config.getString("motd.registered.line2", "&7Welcome back!"); }
    public String getMotdUnregisteredLine1() { return config.getString("motd.unregistered.line1", "&cPlease Register!"); }
    public String getMotdUnregisteredLine2() { return config.getString("motd.unregistered.line2", "&7Use /register to join"); }
    public String getMotdMaintenanceLine1() { return config.getString("motd.maintenance.line1", "&4Maintenance Mode"); }
    public String getMotdMaintenanceLine2() { return config.getString("motd.maintenance.line2", "&7Be back soon!"); }

    // ========== PER-WORLD AUTH ==========
    public boolean isPerWorldAuthEnabled() { return config.getBoolean("per-world-auth.enabled", false); }
    public java.util.List<String> getAuthWorlds() { return config.getStringList("per-world-auth.auth-worlds"); }
    public java.util.List<String> getBypassWorlds() { return config.getStringList("per-world-auth.bypass-worlds"); }
    public boolean isReauthOnWorldSwitch() { return config.getBoolean("per-world-auth.reauth-on-world-switch", false); }

    // ========== MAINTENANCE ==========
    public boolean isMaintenanceEnabled() { return config.getBoolean("maintenance.enabled", false); }
    public void setMaintenanceEnabled(boolean enabled) {
        config.set("maintenance.enabled", enabled);
        plugin.saveConfig();
    }
    public String getMaintenanceKickMessage() { return config.getString("maintenance.kick-message", "&cMaintenance!"); }

    // ========== ANTIBOT ==========
    public boolean isAntiBotEnabled() { return config.getBoolean("antibot.enabled", true); }
    public int getJoinRateLimit() { return config.getInt("antibot.join-rate-limit", 10); }
    public int getLockdownDuration() { return config.getInt("antibot.lockdown-duration", 60); }

    // ========== DISCORD ==========
    public boolean isDiscordEnabled() { return config.getBoolean("discord.enabled", false); }
    public String getDiscordWebhookUrl() { return config.getString("discord.webhook-url", ""); }
    public boolean isDiscordEventEnabled(String event) {
        return config.getBoolean("discord.events." + event, false);
    }

    // ========== ADMIN ALERTS ==========
    public boolean isAdminAlertsEnabled() { return config.getBoolean("admin-alerts.enabled", true); }
    public String getAdminAlertPermission() { return config.getString("admin-alerts.permission", "nxauth.admin.alerts"); }
    public boolean isAdminAlertEnabled(String type) {
        return config.getBoolean("admin-alerts.alerts." + type, true);
    }
    public String getAdminAlertFormat() { return config.getString("admin-alerts.format", "actionbar"); }

    // ========== BACKUP ==========
    public boolean isBackupEnabled() { return config.getBoolean("backup.enabled", true); }
    public int getBackupIntervalHours() { return config.getInt("backup.interval-hours", 24); }
    public int getBackupKeepLast() { return config.getInt("backup.keep-last", 7); }
    public String getBackupPath() { return config.getString("backup.path", "plugins/NxAuth/backups/"); }
    public boolean isBackupCompressed() { return config.getBoolean("backup.compress", true); }

    // ========== WEB DASHBOARD ==========
    public boolean isWebDashboardEnabled() { return config.getBoolean("web-dashboard.enabled", true); }
    public int getWebPort() { return config.getInt("web-dashboard.port", 8080); }
    public String getWebAdminPassword() { return config.getString("web-dashboard.admin-password", "ChangeMe123!"); }
    public String getJwtSecret() { return config.getString("web-dashboard.jwt-secret", "DefaultSecret"); }
    public int getWebSessionDuration() { return config.getInt("web-dashboard.session-duration", 24); }

    // ========== GENERAL ==========
    public String getDefaultLanguage() { return config.getString("general.default-language", "en"); }
    public boolean isPerPlayerLanguage() { return config.getBoolean("general.per-player-language", true); }
    public String getPrefix() { return config.getString("general.prefix", "&8[&bNxAuth&8] "); }
    public boolean isConsoleLogging() { return config.getBoolean("general.console-logging", true); }
    public boolean isFileLogging() { return config.getBoolean("general.file-logging", true); }

    // Generic config setter (used by web dashboard)
    public void set(String path, Object value) {
        config.set(path, value);
        plugin.saveConfig();
    }

    public Object get(String path) {
        return config.get(path);
    }

    public FileConfiguration getRaw() {
        return config;
    }
}
