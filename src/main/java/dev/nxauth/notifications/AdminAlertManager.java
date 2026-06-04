package dev.nxauth.notifications;

import dev.nxauth.NxAuth;
import org.bukkit.entity.Player;

public class AdminAlertManager {

    private final NxAuth plugin;

    public AdminAlertManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void sendAlert(String type, String message) {
        if (!plugin.getConfigManager().isAdminAlertsEnabled()) return;
        if (!plugin.getConfigManager().isAdminAlertEnabled(type)) return;

        String perm = plugin.getConfigManager().getAdminAlertPermission();
        String format = plugin.getConfigManager().getAdminAlertFormat();
        String colored = colorize(message);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.hasPermission(perm)) continue;
                switch (format.toLowerCase()) {
                    case "actionbar" -> p.sendActionBar(colored);
                    case "title" -> p.sendTitle("", colored, 5, 60, 10);
                    default -> p.sendMessage(colored);
                }
            }
        });

        // Also log to console
        if (plugin.getConfigManager().isConsoleLogging()) {
            plugin.getLogger().info("[Alert] " + message.replace("&", "").replaceAll("[0-9a-fk-or]", ""));
        }
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
