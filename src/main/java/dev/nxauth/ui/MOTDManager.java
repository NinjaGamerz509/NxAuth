package dev.nxauth.ui;

import dev.nxauth.NxAuth;
import org.bukkit.ChatColor;

public class MOTDManager {

    private final NxAuth plugin;

    public MOTDManager(NxAuth plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // Nothing to preload; reads from config each ping
    }

    public String getMotd(boolean isMaintenance) {
        if (!plugin.getConfigManager().isMOTDEnabled()) return null;

        String line1, line2;

        if (isMaintenance) {
            line1 = plugin.getConfigManager().getMotdMaintenanceLine1();
            line2 = plugin.getConfigManager().getMotdMaintenanceLine2();
        } else {
            // Default to registered MOTD (we can't tell from ping who is connecting)
            line1 = plugin.getConfigManager().getMotdRegisteredLine1();
            line2 = plugin.getConfigManager().getMotdRegisteredLine2();
        }

        line1 = replacePlaceholders(line1);
        line2 = replacePlaceholders(line2);

        return colorize(line1 + "\n" + line2);
    }

    private String replacePlaceholders(String text) {
        int online = plugin.getServer().getOnlinePlayers().size();
        int max = plugin.getServer().getMaxPlayers();
        long registered = plugin.getDatabaseManager().getTotalPlayers();

        return text
            .replace("{online}", String.valueOf(online))
            .replace("{max}", String.valueOf(max))
            .replace("{registered}", String.valueOf(registered));
    }

    private String colorize(String text) {
        return text.replace("&", "\u00A7");
    }
}
