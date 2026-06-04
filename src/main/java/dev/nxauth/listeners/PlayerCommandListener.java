package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

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
