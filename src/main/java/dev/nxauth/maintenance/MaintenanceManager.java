package dev.nxauth.maintenance;

import dev.nxauth.NxAuth;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MaintenanceManager {

    private final NxAuth plugin;

    public MaintenanceManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().isMaintenanceEnabled();
    }

    public void setEnabled(boolean enabled, CommandSender sender) {
        plugin.getConfigManager().setMaintenanceEnabled(enabled);
        String state = enabled ? "enabled" : "disabled";

        // Notify admins
        String msg = colorize("&e[NxAuth] Maintenance mode " + (enabled ? "&cENABLED" : "&aEND") + " &eby &f" + sender.getName());
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("nxauth.admin")) p.sendMessage(msg);
        }
        sender.sendMessage(colorize("&aMaintenance mode " + state + "!"));

        // Kick non-admin players if enabling
        if (enabled) {
            String kickMsg = colorize(plugin.getConfigManager().getMaintenanceKickMessage());
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.hasPermission("nxauth.admin.maintenance.bypass")) {
                    p.kickPlayer(kickMsg);
                }
            }
        }

        // Discord notification
        plugin.getDiscordWebhook().sendAsync("maintenance-toggle",
            "Maintenance " + state + " by " + sender.getName(), "", enabled);
    }

    private String colorize(String s) { return s.replace("&", "\u00A7"); }
}
