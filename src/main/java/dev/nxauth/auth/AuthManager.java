package dev.nxauth.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dev.nxauth.NxAuth;
import dev.nxauth.config.ConfigManager;
import dev.nxauth.config.MessageManager;
import dev.nxauth.ui.SoundManager;
import dev.nxauth.ui.AnimationManager;
import org.bukkit.entity.Player;

import java.util.*;

public class AuthManager {

    private final NxAuth plugin;
    private final Set<UUID> loggedIn = new HashSet<>();
    private final Map<UUID, Integer> failedAttempts = new HashMap<>();
    private final Map<UUID, Long> loginTime = new HashMap<>();
    // Players waiting for 2FA after password check
    private final Set<UUID> pending2FA = new HashSet<>();
    // Players waiting for CAPTCHA
    private final Map<UUID, CaptchaData> pendingCaptcha = new HashMap<>();

    public AuthManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    // ==================== REGISTER ====================

    public RegisterResult register(Player player, String password, String confirm) {
        ConfigManager cfg = plugin.getConfigManager();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();

        if (isLoggedIn(player)) return RegisterResult.ALREADY_LOGGED_IN;
        if (plugin.getDatabaseManager().isRegistered(uuid)) return RegisterResult.ALREADY_REGISTERED;

        // Password checks
        if (!password.equals(confirm)) return RegisterResult.PASSWORD_MISMATCH;
        if (password.length() < cfg.getMinPasswordLength()) return RegisterResult.TOO_SHORT;
        if (password.length() > cfg.getMaxPasswordLength()) return RegisterResult.TOO_LONG;
        if (cfg.isBlockWeakPasswords() && isWeakPassword(password)) return RegisterResult.WEAK_PASSWORD;

        // IP limit check
        if (!cfg.isAllowMultiplePerIp()) {
            int accountsOnIp = plugin.getDatabaseManager().countAccountsByIp(ip);
            int maxAllowed = cfg.getMaxAccountsPerIp();
            if (maxAllowed > 0 && accountsOnIp >= maxAllowed) return RegisterResult.IP_LIMIT;
        }

        // Hash password
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        // Save to DB
        boolean saved = plugin.getDatabaseManager().registerPlayer(uuid, player.getName(), hash, ip);
        if (!saved) return RegisterResult.DATABASE_ERROR;

        // Log event
        plugin.getDatabaseManager().logEvent(uuid, player.getName(), "REGISTER", ip, true, "New registration");

        // Login after register
        setLoggedIn(player, true);
        loginTime.put(player.getUniqueId(), System.currentTimeMillis());

        // Effects
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            JoinScreenManager joinScreen = new JoinScreenManager(plugin);
            joinScreen.onLoginSuccess(player);

            // First-time firework
            if (cfg.isRegisterFireworkEnabled()) {
                AnimationManager.spawnRegisterFirework(player);
            }

            // Sound
            SoundManager.playSound(player, cfg.getSoundRegisterFirst(), cfg.getSoundVolume(), cfg.getSoundPitch());
        });

        // Notifications
        plugin.getAdminAlertManager().sendAlert("new-register",
            "&a[NxAuth] &f" + player.getName() + " &ajust registered! IP: " + ip);
        plugin.getDiscordWebhook().sendAsync("new-register", player.getName(), ip, true);

        return RegisterResult.SUCCESS;
    }

    // ==================== LOGIN ====================

    public LoginResult login(Player player, String password) {
        ConfigManager cfg = plugin.getConfigManager();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();

        if (isLoggedIn(player)) return LoginResult.ALREADY_LOGGED_IN;
        if (!plugin.getDatabaseManager().isRegistered(uuid)) return LoginResult.NOT_REGISTERED;

        // Check brute force
        if (plugin.getBruteForceProtection().isBlocked(ip)) return LoginResult.BRUTE_FORCE_BLOCKED;

        // Verify password
        String hash = plugin.getDatabaseManager().getPasswordHash(uuid);
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);

        if (!result.verified) {
            // Failed attempt
            int attempts = failedAttempts.getOrDefault(player.getUniqueId(), 0) + 1;
            failedAttempts.put(player.getUniqueId(), attempts);
            plugin.getBruteForceProtection().recordFailedAttempt(ip);

            // Log
            plugin.getDatabaseManager().logEvent(uuid, player.getName(), "LOGIN_FAIL", ip, false,
                "Attempt " + attempts + "/" + cfg.getMaxAttempts());

            // Discord/Admin alert
            plugin.getAdminAlertManager().sendAlert("failed-login",
                "&c[NxAuth] Failed login by &f" + player.getName() + " &c(IP: " + ip + ") Attempt " + attempts);

            if (attempts >= cfg.getMaxAttempts()) {
                // Tempban
                plugin.getBruteForceProtection().tempban(ip, cfg.getTempbanDuration());
                plugin.getDatabaseManager().logEvent(uuid, player.getName(), "TEMPBAN", ip, false,
                    "Max attempts reached");
                plugin.getAdminAlertManager().sendAlert("max-attempts",
                    "&4[NxAuth] &c" + player.getName() + " &4tempbanned for " + cfg.getTempbanDuration() + " min!");
                plugin.getDiscordWebhook().sendAsync("max-attempts-reached", player.getName(), ip, false);
                return LoginResult.MAX_ATTEMPTS;
            }

            SoundManager.playSound(player, cfg.getSoundLoginFail(), cfg.getSoundVolume(), cfg.getSoundPitch());
            return LoginResult.WRONG_PASSWORD;
        }

        // Password correct - check 2FA
        if (plugin.getDatabaseManager().is2faEnabled(uuid)) {
            pending2FA.add(player.getUniqueId());
            return LoginResult.NEEDS_2FA;
        }

        // Complete login
        completeLogin(player, ip);
        return LoginResult.SUCCESS;
    }

    public void completeLogin(Player player, String ip) {
        String uuid = player.getUniqueId().toString();

        failedAttempts.remove(player.getUniqueId());
        pending2FA.remove(player.getUniqueId());
        setLoggedIn(player, true);
        loginTime.put(player.getUniqueId(), System.currentTimeMillis());

        plugin.getDatabaseManager().updateLastLogin(uuid, ip);
        plugin.getDatabaseManager().logEvent(uuid, player.getName(), "LOGIN", ip, true, "Successful login");

        // Remove bossbar
        plugin.getBossBarManager().removeBossBar(player);

        // Effects
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            JoinScreenManager joinScreen = new JoinScreenManager(plugin);
            joinScreen.onLoginSuccess(player);

            ConfigManager cfg = plugin.getConfigManager();
            SoundManager.playSound(player, cfg.getSoundLoginSuccess(), cfg.getSoundVolume(), cfg.getSoundPitch());

            if (cfg.isLoginParticlesEnabled()) {
                AnimationManager.spawnLoginParticles(player, cfg.getLoginParticleType(), cfg.getLoginParticleCount());
            }

            if (cfg.isCinematicEnabled()) {
                AnimationManager.playCinematic(player);
            }
        });
    }

    // ==================== LOGOUT ====================

    public boolean logout(Player player) {
        if (!isLoggedIn(player)) return false;

        setLoggedIn(player, false);
        loginTime.remove(player.getUniqueId());

        // Re-apply auth restrictions
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            JoinScreenManager joinScreen = new JoinScreenManager(plugin);
            joinScreen.applyAuthRestrictions(player);
            plugin.getBossBarManager().showBossBar(player);
        });

        return true;
    }

    // ==================== HELPERS ====================

    public boolean isLoggedIn(Player player) {
        return loggedIn.contains(player.getUniqueId());
    }

    public void setLoggedIn(Player player, boolean state) {
        if (state) loggedIn.add(player.getUniqueId());
        else loggedIn.remove(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        loggedIn.remove(player.getUniqueId());
        failedAttempts.remove(player.getUniqueId());
        loginTime.remove(player.getUniqueId());
        pending2FA.remove(player.getUniqueId());
        pendingCaptcha.remove(player.getUniqueId());
    }

    public boolean isPending2FA(Player player) {
        return pending2FA.contains(player.getUniqueId());
    }

    public boolean hasPendingCaptcha(Player player) {
        return pendingCaptcha.containsKey(player.getUniqueId());
    }

    public CaptchaData getCaptcha(Player player) {
        return pendingCaptcha.get(player.getUniqueId());
    }

    public void setPendingCaptcha(Player player, CaptchaData data) {
        pendingCaptcha.put(player.getUniqueId(), data);
    }

    public void removeCaptcha(Player player) {
        pendingCaptcha.remove(player.getUniqueId());
    }

    public int getRemainingAttempts(Player player) {
        int max = plugin.getConfigManager().getMaxAttempts();
        int used = failedAttempts.getOrDefault(player.getUniqueId(), 0);
        return max - used;
    }

    public long getLoginTime(Player player) {
        return loginTime.getOrDefault(player.getUniqueId(), 0L);
    }

    public Set<UUID> getLoggedInPlayers() {
        return Collections.unmodifiableSet(loggedIn);
    }

    private boolean isWeakPassword(String password) {
        Set<String> commonPasswords = Set.of(
            "123456", "password", "123456789", "12345678", "12345",
            "111111", "1234567", "sunshine", "qwerty", "iloveyou",
            "princess", "admin", "welcome", "666666", "abc123",
            "football", "123123", "monkey", "654321", "!@#$%^&*",
            "charlie", "donald", "password1", "qwerty123"
        );
        return commonPasswords.contains(password.toLowerCase());
    }

    // ==================== ENUMS ====================

    public enum RegisterResult {
        SUCCESS, ALREADY_LOGGED_IN, ALREADY_REGISTERED, PASSWORD_MISMATCH,
        TOO_SHORT, TOO_LONG, WEAK_PASSWORD, IP_LIMIT, DATABASE_ERROR
    }

    public enum LoginResult {
        SUCCESS, ALREADY_LOGGED_IN, NOT_REGISTERED, WRONG_PASSWORD,
        MAX_ATTEMPTS, BRUTE_FORCE_BLOCKED, NEEDS_2FA
    }

    // ==================== INNER CLASS ====================

    public static class CaptchaData {
        public final int answer;
        public final String question;
        public final long expiresAt;
        public int fails = 0;

        public CaptchaData(String question, int answer, int timeoutSeconds) {
            this.question = question;
            this.answer = answer;
            this.expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
