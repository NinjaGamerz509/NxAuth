package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

// ============================================================
// PlayerQuitListener
// ============================================================
public class PlayerQuitListener implements Listener {
    private final NxAuth plugin;
    public PlayerQuitListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Save session if logged in
        if (plugin.getAuthManager().isLoggedIn(player)) {
            String ip = player.getAddress().getAddress().getHostAddress();
            plugin.getSessionManager().saveSession(
                player.getUniqueId().toString(), ip,
                plugin.getConfigManager().getIpSessionDuration());
        }
        new JoinScreenManager(plugin).onPlayerQuit(player);
        plugin.getAuthManager().removePlayer(player);
        plugin.getSessionManager().removePlayer(player);
        plugin.getBossBarManager().removeBossBar(player);
    }
}

// ============================================================
// PlayerMoveListener - freeze unauthenticated players
// ============================================================
public class PlayerMoveListener implements Listener {
    private final NxAuth plugin;
    public PlayerMoveListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        plugin.getSessionManager().updateMovement(player);

        if (!plugin.getAuthManager().isLoggedIn(player)) {
            JoinScreenManager screen = new JoinScreenManager(plugin);
            if (screen.isFrozen(player)) {
                // Allow head movement but cancel position change
                if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

// ============================================================
// PlayerChatListener - intercept chat for captcha / 2fa
// ============================================================
public class PlayerChatListener implements Listener {
    private final NxAuth plugin;
    public PlayerChatListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }
}

// ============================================================
// PlayerCommandListener - block non-auth commands
// ============================================================
public class PlayerCommandListener implements Listener {
    private final NxAuth plugin;
    private static final java.util.Set<String> ALLOWED = java.util.Set.of(
        "login", "l", "log", "register", "reg", "2fa", "language", "lang"
    );

    public PlayerCommandListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isLoggedIn(player)) return;

        String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();
        if (!ALLOWED.contains(command)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(player,
                plugin.getDatabaseManager().isRegistered(player.getUniqueId().toString())
                    ? "login.prompt" : "register.prompt");
        }
    }
}

// ============================================================
// ServerPingListener - MOTD changer
// ============================================================
public class ServerPingListener implements Listener {
    private final NxAuth plugin;
    public ServerPingListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        boolean maintenance = plugin.getMaintenanceManager().isEnabled();
        String motd = plugin.getMotdManager().getMotd(maintenance);
        if (motd != null) event.setMotd(motd);
    }
}

// ============================================================
// WorldChangeListener - per-world auth
// ============================================================
public class WorldChangeListener implements Listener {
    private final NxAuth plugin;
    public WorldChangeListener(NxAuth plugin) { this.plugin = plugin; }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!plugin.getConfigManager().isPerWorldAuthEnabled()) return;

        Player player = event.getPlayer();
        String newWorld = player.getWorld().getName();
        java.util.List<String> authWorlds = plugin.getConfigManager().getAuthWorlds();
        java.util.List<String> bypassWorlds = plugin.getConfigManager().getBypassWorlds();

        // Entering an auth world from a bypass world
        if (authWorlds.contains(newWorld) && bypassWorlds.contains(event.getFrom().getName())) {
            if (plugin.getConfigManager().isReauthOnWorldSwitch()
                && plugin.getAuthManager().isLoggedIn(player)) {
                plugin.getAuthManager().logout(player);
                plugin.getMessageManager().sendMessage(player, "login.prompt");
                new JoinScreenManager(plugin).applyAuthRestrictions(player);
                plugin.getBossBarManager().showBossBar(player);
            }
        }
    }
}
