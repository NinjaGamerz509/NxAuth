package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import dev.nxauth.premium.PremiumManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {

    private final NxAuth plugin;

    public PlayerJoinListener(NxAuth plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String ip = event.getAddress().getHostAddress();

        // Maintenance mode check
        if (plugin.getMaintenanceManager().isEnabled()) {
            if (!player.hasPermission("nxauth.admin.maintenance.bypass")) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    colorize(plugin.getConfigManager().getMaintenanceKickMessage()));
                return;
            }
        }

        // Anti-bot check
        if (plugin.getAntiBotManager().isLocked()) {
            if (!player.hasPermission("nxauth.admin")) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    colorize("&cServer is in lockdown due to bot attack. Try again later."));
                return;
            }
        }

        // IP ban check
        if (plugin.getDatabaseManager().isIpBanned(ip)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, colorize("&cYour IP is banned from this server."));
            return;
        }

        // Record join for antibot
        plugin.getAntiBotManager().recordJoin(ip);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Bedrock auto-login
        if (plugin.getConfigManager().isBedrockAutoLogin()
            && plugin.getPremiumManager().isBedrockPlayer(player)) {
            plugin.getAuthManager().setLoggedIn(player, true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMessageManager().sendMessage(player, "login.success",
                    java.util.Map.of("player", player.getName()));
            }, 20L);
            return;
        }

        // Premium auto-login
        if (plugin.getConfigManager().isPremiumEnabled()
            && plugin.getPremiumManager().isPremiumPlayer(player)) {
            plugin.getAuthManager().setLoggedIn(player, true);
            plugin.getDatabaseManager().updateLastLogin(uuid, ip);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMessageManager().sendMessage(player, "login.success",
                    java.util.Map.of("player", player.getName()));
            }, 20L);
            return;
        }

        // IP Session check (auto-login if same IP)
        if (plugin.getSessionManager().hasValidSession(player)) {
            plugin.getAuthManager().setLoggedIn(player, true);
            plugin.getDatabaseManager().updateLastLogin(uuid, ip);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMessageManager().sendMessage(player, "login.success",
                    java.util.Map.of("player", player.getName()));
                new JoinScreenManager(plugin).onLoginSuccess(player);
            }, 20L);
            return;
        }

        // Apply auth restrictions
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            new JoinScreenManager(plugin).applyAuthRestrictions(player);
            plugin.getBossBarManager().showBossBar(player);

            // Send prompt message
            boolean isRegistered = plugin.getDatabaseManager().isRegistered(uuid);
            plugin.getMessageManager().sendMessage(player,
                isRegistered ? "login.prompt" : "register.prompt");

            // Show CAPTCHA if configured for login
            String captchaOn = plugin.getConfigManager().getCaptchaShowOn();
            if (plugin.getConfigManager().isCaptchaEnabled()
                && (captchaOn.equals("login") || captchaOn.equals("both"))
                && !player.hasPermission("nxauth.bypass.captcha")) {
                plugin.getCaptchaManager().sendCaptcha(player,
                    isRegistered ? "login" : "register");
            }
        }, 10L);
    }

    private String colorize(String s) {
        return s.replace("&", "\u00A7");
    }
}
