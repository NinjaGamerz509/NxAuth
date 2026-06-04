package dev.nxauth.listeners;

import dev.nxauth.NxAuth;
import dev.nxauth.auth.JoinScreenManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.entity.Player;

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
